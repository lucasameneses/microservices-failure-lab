# Failure Lab API walkthrough

How to drive the lab over HTTP: which endpoint to call, in what order, and what each answer
means. `lab.sh` makes exactly these calls. Read this before writing your own client (a script,
a k6 test, a control panel).

Every path, field and error code below was copied from the controllers in `services/` and checked
against the running lab on 2026-11-11.

## Who you talk to

- **service-a**, `http://localhost:8080`: the entry point (`/flow`) and the `cycle` and
  `resource-exhaustion` scenarios.
- **service-b**, `http://localhost:8081`: the `latency` scenario.

A client talks to both, because each service owns its own scenarios. `/flow` and `/cycle` then
call the other service behind the scenes. The client never calls `service-b /process` itself.

- **Auth**: none. The lab is local-only. Do not expose these ports to a network.
- **Content type**: `application/json` on every request that has a body. Bodies are always
  optional.
- **State**: scenario flags live in memory. Restarting a service turns its scenarios off.

The two network scenarios (`tc netem` and stopping service-b) are not HTTP calls. `lab.sh` runs
them with `docker compose`, so they are out of this document. Step 1 shows how they look from
the client's side.

```
[1] GET  /flow                         A     the traffic you observe (k6 sends 5 req/s)
 |
[2] GET  /chaos                        A, B  read which scenarios are on
 |
[3] POST /chaos/{scenario}             A or B  turn one scenario on
 |      latency               -> B
 |      cycle                 -> A
 |      resource-exhaustion   -> A
 |
[4] GET  /flow                         A     watch the failure (Grafana, Jaeger, Loki)
 |
[5] DELETE /chaos                      A, B  turn everything off
```

## Step 1 — Call the normal flow

```
GET http://localhost:8080/flow
```

- **Roles**: none.
- **Request body**: none.

Response `200`, no scenario active:

```json
{
  "service": "service-a",
  "downstream": {              // <-- body returned by service-b GET /process, passed through as is
    "service": "service-b",
    "status": "OK"
  }
}
```

With `cycle` active (step 3), `downstream` is instead the body of service-b `GET /cycle?depth=1`.
That body nests 10 levels deep (see step 3b).

| Field | Type | Meaning |
|---|---|---|
| `service` | string | Always `service-a`. |
| `downstream` | object | Whatever service-b answered. Its shape depends on the active scenario. |

> `downstream` is untyped JSON on purpose. Do not bind it to a fixed class: it is
> `{service, status}` in the normal flow and the recursive cycle object while `cycle` is on.

Errors:

| Status | `error` | When | Message to show |
|---|---|---|---|
| 504 | `DOWNSTREAM_TIMEOUT` | service-b did not connect within 1 s or answer within 3 s (`tc`, or latency above 3000 ms) | "service-b did not answer in time." |
| 502 | `DOWNSTREAM_ERROR` | service-b is down, refused the call or answered 5xx | "service-b is unavailable." |

> **Not a bug:** with service-b stopped, the `message` field reads
> `service-b call failed: service-b`. The underlying exception is `UnknownHostException`, whose
> message is only the host name, because Docker removes the stopped container's DNS entry. Show
> your own sentence, not `message`.

> **Not a bug:** with service-b stopped, some `/flow` calls take 5 to 10 s before the 502
> instead of failing at once. The cost is the DNS lookup of a name that no longer exists. Give
> your client a timeout above 10 s, or it will record these as its own timeouts.

```
       |
       v
```

## Step 2 — Read which scenarios are on

```
GET http://localhost:8080/chaos
GET http://localhost:8081/chaos
```

- **Roles**: none.
- **Request body**: none.

service-a, `200`:

```json
{
  "service": "service-a",
  "active": {
    "cycle": false,                 // <-- turned on by POST /chaos/cycle (step 3b)
    "resource-exhaustion": false    // <-- true while the job runs; goes false by itself after the OOM
  }
}
```

service-b, `200`:

```json
{
  "service": "service-b",
  "active": {
    "latency": false   // <-- turned on by POST /chaos/latency (step 3a)
  },
  "delayMs": 0         // <-- 0 whenever latency is off
}
```

| Field | Type | Meaning |
|---|---|---|
| `service` | string | `service-a` or `service-b`. |
| `active` | object of booleans | One key per scenario this service owns. |
| `delayMs` | integer | service-b only. The injected delay, `0` when off. |

> `active` keys contain a hyphen (`resource-exhaustion`). In JavaScript read it as
> `active["resource-exhaustion"]`, not with dot notation.

> The two `active` objects have different keys. Merge them on the client if you want a single
> view; neither service knows the other's scenarios.

Errors: none.

```
       |
       v
```

## Step 3 — Turn a scenario on

Every activation answers with the same body as step 2 for that service, already updated. Keep it
as the new state: no need to call step 2 again.

### Step 3a — `latency` (service-b)

```
POST http://localhost:8081/chaos/latency
Content-Type: application/json
```

- **Roles**: none.

```json
{
  "delayMs": 2000   // <-- optional; omit the body to get 2000
}
```

| Field | Type | Required | Meaning |
|---|---|---|---|
| `delayMs` | integer | no | Delay before each `/process` answer, 1 to 10000 ms. Default 2000. |

Response `200`:

```json
{
  "service": "service-b",
  "active": { "latency": true },
  "delayMs": 2000   // <-- echoes what is in force; read it back instead of trusting what you sent
}
```

- Calling it again while active replaces the delay. It does not answer `409`.
- `delayMs: 0` does not turn latency off. It is rejected; use step 5.

> **Trap:** service-a gives up on service-b after 3 s (`lab.downstream.read-timeout`). With
> `delayMs` above about 3000, `/flow` stops showing slowness and returns `504 DOWNSTREAM_TIMEOUT`
> on every call. Use values below 3000 to show latency; values above it show timeouts instead.

Errors:

| Status | `error` | When | Message to show |
|---|---|---|---|
| 400 | `INVALID_PARAMETER` | `delayMs` outside 1–10000 | "The delay must be between 1 and 10000 ms." |
| 400 | `INVALID_PARAMETER` | body is not valid JSON, or `delayMs` is not a number | "Invalid request." |
| 404 | `UNKNOWN_SCENARIO` | any other name, e.g. `cycle` sent to service-b | "This service has no scenario with that name." |

### Step 3b — `cycle` (service-a)

```
POST http://localhost:8080/chaos/cycle
```

- **Roles**: none.
- **Request body**: none. A body sent anyway is ignored, but it must be valid JSON or the call
  answers `400 INVALID_PARAMETER`.

Response `200`:

```json
{
  "service": "service-a",
  "active": { "cycle": true, "resource-exhaustion": false }
}
```

Calling it again while active is a no-op with the same answer.

From now on, `/flow` (step 4) answers with the whole cycle, one object per hop:

```json
{
  "service": "service-a",
  "downstream": {
    "service": "service-b",
    "depth": 1,
    "limitReached": false,
    "downstream": {
      "service": "service-a",
      "depth": 2,
      "limitReached": false,
      "downstream": { "...": "..." }   // <-- continues, alternating services, until depth 10
    }
  }
}
```

The innermost object has `"depth": 10`, `"limitReached": true` and `"downstream": null`.

| Field | Type | Meaning |
|---|---|---|
| `depth` | integer | Position of this hop in the cycle. |
| `limitReached` | boolean | `true` only on the last hop, where the cycle stops. |
| `downstream` | object or `null` | The next hop, or `null` on the last one. |

> **Not a bug:** the cycle ends with `200`, not an error. The limit is the lab's guard
> (`lab.cycle.max-depth=10`); the failure is the 10 × request amplification you see in Grafana
> and Jaeger, not a status code.

Errors:

| Status | `error` | When | Message to show |
|---|---|---|---|
| 400 | `INVALID_PARAMETER` | a body was sent and is not valid JSON | "Invalid request." |
| 404 | `UNKNOWN_SCENARIO` | any other name, e.g. `latency` sent to service-a | "This service has no scenario with that name." |

### Step 3c — `resource-exhaustion` (service-a)

```
POST http://localhost:8080/chaos/resource-exhaustion
Content-Type: application/json
```

- **Roles**: none.

```json
{
  "chunkMb": 2,       // <-- optional
  "intervalMs": 500,  // <-- optional
  "cpuThreads": 2     // <-- optional
}
```

| Field | Type | Required | Meaning |
|---|---|---|---|
| `chunkMb` | integer | no | Heap allocated and kept each tick, 1 to 64 MB. Default 2. |
| `intervalMs` | integer | no | Time between allocations, 50 to 5000 ms. Default 500. |
| `cpuThreads` | integer | no | Threads busy-looping on math, 1 to 8. Default 2. |

Each field is validated and defaulted on its own: `{"chunkMb": 4}` keeps the other two at their
defaults.

Response `200`:

```json
{
  "service": "service-a",
  "active": { "cycle": false, "resource-exhaustion": true }   // <-- poll step 2 to see it end
}
```

This scenario is asynchronous. Its lifecycle:

| `active["resource-exhaustion"]` | Meaning | Terminal |
|---|---|---|
| `true` | The job is allocating heap and burning CPU. | no |
| `false` after a real `OutOfMemoryError` | The job caught the OOM, logged it (ERROR in Loki), released the memory and stopped. With the defaults this takes about one minute. | yes |
| `false` after step 5 | Stopped by reset, memory released, no error logged. | yes |

- Poll step 2 every 5 s (the Prometheus scrape interval) to follow it.
- The service keeps answering throughout. `/flow` becomes slower, not unavailable.
- There is no callback. The OOM appears only in the flag, the Loki log and the heap panel.

Errors:

| Status | `error` | When | Message to show |
|---|---|---|---|
| 400 | `INVALID_PARAMETER` | a field out of range; `message` names the field, e.g. `chunkMb must be between 1 and 64` | "<field> must be between <min> and <max>." (`message` is safe to show as is) |
| 400 | `INVALID_PARAMETER` | body is not valid JSON, or a field is not a number | "Invalid request." |
| 409 | `SCENARIO_ALREADY_RUNNING` | the job is already running | "Resource exhaustion is already running. Wait for the OOM or reset." |

```
       |
       v
```

## Step 4 — Watch the failure

Keep calling step 1 (k6 already does, at 5 req/s). What each scenario does to `/flow`:

| Scenario | `/flow` status | `/flow` body | Where to look |
|---|---|---|---|
| none | 200 | `{service, status}` downstream | — |
| `latency` (≤ 3000 ms) | 200, about `delayMs` slower | unchanged | Grafana p95 panels, Jaeger `/process` span |
| `latency` (> 3000 ms) | 504 `DOWNSTREAM_TIMEOUT` | error body | Grafana 5xx panel |
| `cycle` | 200 | the nested cycle | Grafana request rate, Jaeger trace of 21 spans |
| `resource-exhaustion` | 200, slower | unchanged | Grafana CPU and heap panels, Loki ERROR |
| `tc netem` (via `lab.sh`) | mostly 200, some 504 | — | Grafana 5xx and p95, Jaeger client span |
| service-b stopped (via `lab.sh`) | 502 `DOWNSTREAM_ERROR` | error body | Grafana 5xx, Jaeger with no service-b span |

Every `/flow` request produces one trace, and its logs in Loki carry the same `trace_id`. The
response carries no trace id. To find the trace for a given call, search Jaeger by time and
service.

```
       |
       v
```

## Step 5 — Turn everything off

```
DELETE http://localhost:8080/chaos
DELETE http://localhost:8081/chaos
```

- **Roles**: none.
- **Request body**: none.

Response `200`, same shape as step 2 with every flag off:

```json
{ "service": "service-a", "active": { "cycle": false, "resource-exhaustion": false } }
```

```json
{ "service": "service-b", "active": { "latency": false }, "delayMs": 0 }
```

- Call both. Each service only resets its own scenarios.
- It is idempotent: calling it with nothing active is fine.
- It stops a running `resource-exhaustion` job and frees its memory.
- It does not undo `tc` or restart a stopped service-b. `./lab.sh reset` does all of it.

Errors: none.

## Quick reference

### Error body

Every error the services raise themselves:

```json
{ "error": "DOWNSTREAM_TIMEOUT", "message": "service-b did not answer in time" }
```

`error` is a stable code to branch on. `message` is for developers and may change.

> **Trap:** errors raised by Spring itself use a different body:
> `{"timestamp", "status", "error", "path"}`, where `error` is a reason phrase such as
> `Not Found`, not a code. This happens on an unknown path (`404`), a wrong method such as
> `POST /chaos` (`405`) and a body sent with a content type other than `application/json`
> (`415`). Branch on the HTTP status first, then on `error`.

### Error codes

| Status | `error` | Service | Step |
|---|---|---|---|
| 400 | `INVALID_PARAMETER` | A, B | 3a, 3b, 3c; also `GET /cycle?depth=` negative or not a number |
| 404 | `UNKNOWN_SCENARIO` | A, B | 3a, 3b, 3c (scenario sent to the wrong service) |
| 409 | `SCENARIO_ALREADY_RUNNING` | A | 3c |
| 502 | `DOWNSTREAM_ERROR` | A, B | 1, and every hop of the cycle |
| 504 | `DOWNSTREAM_TIMEOUT` | A, B | 1, and every hop of the cycle |

### Internal endpoint: `GET /cycle`

Both services expose `GET /cycle?depth=N` (`depth` integer ≥ 0, default 0). It is the hop the
services call on each other while `cycle` is active. You can call it directly to run one cycle
without turning the scenario on: `GET http://localhost:8080/cycle` runs the full 10 hops.
`depth` 10 or more answers at once with `limitReached: true`.

> When a hop deep in the cycle fails, the services above it answer `502 DOWNSTREAM_ERROR`, even
> if the original failure was a timeout: a 5xx from the peer is always mapped to 502. Only the
> hop that hit the timeout says `DOWNSTREAM_TIMEOUT`.

### Other endpoints

`GET /actuator/health` and `GET /actuator/prometheus` on both services, standard Spring Boot
Actuator. Prometheus scrapes the second; `lab.sh` uses the first to wait for service-b.

### Before and after

The lab replaced the prototype's endpoints. A client written against the old ones must change:

| Before | After |
|---|---|
| `GET /hello` (A, B) | removed; use `GET /actuator/health` |
| `GET /call-b` (A) | `GET /flow` |
| `POST /actuator/failure` (B) | `POST /chaos/latency` on B, or `POST /chaos/{cycle,resource-exhaustion}` on A |
| `POST /actuator/reset` (B) | `DELETE /chaos` on each service |
| — | `GET /chaos`, `GET /cycle`, `GET /process` (B, called by A only) |

### Open points

1. **Latency above 3 s turns into timeouts.** `delayMs` accepts up to 10000, but service-a's read
   timeout is 3 s, so any value above about 3000 produces `504` instead of slowness. The READMEs
   now use `./lab.sh latency 2500` as the example and warn about it; the cap itself is unchanged.
2. **The service-b-down message hides its cause.** `DOWNSTREAM_ERROR` carries
   `service-b call failed: service-b`, because `UnknownHostException`'s message is just the host
   name. Known, low priority.
3. **A 504 deep in the cycle surfaces as 502.** Every hop above the failing one sees a 5xx from
   its peer and maps it to `DOWNSTREAM_ERROR`, so the root response loses the timeout. This is
   acceptable for the paper's scenarios, which never combine `cycle` with network failures, but
   a client cannot tell the two causes apart from the status alone.
4. **Two error body shapes.** Spring's own 404/405/415 do not use `{error, message}`. Low impact
   for a local lab; noted for completeness.
5. **Slow 502s with service-b down.** The DNS lookup makes some calls take 5–10 s before failing
   (known, see step 1). Clients need a timeout above 10 s.
