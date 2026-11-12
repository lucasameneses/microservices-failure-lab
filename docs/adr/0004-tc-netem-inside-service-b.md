# 0004 — Network faults use tc netem inside the service-b container

- **Status**: accepted
- **Context**: The paper says network faults were produced with Linux `tc` (delay and packet
  loss) and by stopping service-b.
- **Decision**: The service-b image includes `iproute2` and the container gets `NET_ADMIN`. The
  lab script applies `tc qdisc add dev eth0 root netem ...` with `docker compose exec` and removes
  it with `tc qdisc del`. Stopping service-b uses `docker compose stop` / `start`.
- **Alternatives**:
  - Pumba: one more tool and it needs the Docker socket mounted.
  - `tc` on the host: needs root on the host and affects other traffic.
- **Consequences**: No extra tool. Needs a Linux kernel (Linux host or Docker Desktop VM). The
  delay applies to all egress of service-b, including its telemetry export, which is acceptable.
