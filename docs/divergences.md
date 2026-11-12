# Divergences

Claims of the approved paper, listed in the [credibility checklist](credibility-checklist.md), that
running the lab did not confirm, even with the simulation set up as the paper describes. The
evidence wins: the text was corrected, never the telemetry.

The last column is the wording now in the revised [`paper.pdf`](paper.pdf). Slide numbers refer
to the deck shown at the defense. The published [`slides.pdf`](slides.pdf) was rebuilt from the
revised paper and uses the corrected wording.

| Claim | What the approved paper says | What was observed | Evidence | Revised text in `paper.pdf` |
|---|---|---|---|---|
| C02 | §3: OTel "atuou como um padrão unificado para a geração de telemetria, incluindo métricas, logs e, principalmente, rastreamentos distribuídos" | Traces and logs go through OpenTelemetry (OTLP → Collector → Jaeger and Loki). Metrics do not: Micrometer exposes them and Prometheus scrapes `/actuator/prometheus`, as the same section 3 says in the Prometheus item. | [cycle-loki](img/cycle-loki.png), [cycle-jaeger](img/cycle-jaeger.png) | §3: "O OTel é um padrão unificado para a geração de telemetria, incluindo métricas, logs e, principalmente, rastreamentos distribuídos (traces). Neste ambiente, ele foi empregado para os rastreamentos distribuídos e os logs, enviados por um OpenTelemetry Collector ao Jaeger e ao Loki; as métricas foram expostas pelo Micrometer e coletadas pelo Prometheus." §2 now speaks of OTel in general ("permite que um sistema produza"). |
| C09 | §4.2, slide 24: "Os logs centralizados no Loki registraram eventos de timeout e tentativas de retentativas excedidas" | The cycle ends cleanly at the call limit. Loki shows one `Cycle hop depth=N` per hop and the WARN `Cycle limit reached at depth 10 (max 10)`, all under the trace's `trace_id`. There is no timeout and no retry, because the lab has no retry policy. | [cycle-loki](img/cycle-loki.png) | §4.2: "Os logs centralizados no Loki registraram cada salto do ciclo e o evento de limite atingido, todos com o mesmo identificador de rastreamento, confirmando a presença da dependência cíclica [...]", and the following paragraph mentions "os registros de cada salto e do limite atingido no Loki". |
| C18 | §4.4, slide 26: "Os logs registrados pelo Loki complementaram a análise, destacando erros como ConnectionRefused e GatewayTimeout." | With `tc`: `Read timed out` / `Connect timed out`, answered as 504 (GatewayTimeout) — confirmed. With service-b stopped: Docker removes the container's DNS name, so the error is `UnknownHostException` (message `service-b`), answered as 502. `ConnectionRefused` does not appear. | [network-loki](img/network-loki.png) | §4.4: "Os logs registrados pelo Loki complementaram a análise, destacando erros de tempo esgotado (GatewayTimeout) sob atraso e perda de pacotes e de host inalcançável (UnknownHostException) quando o Microsserviço B foi desligado." |

## Not changed

Section 3 also states what each tool *would* show, before the experiment ("os logs centralizados
no Loki registrariam erros como timeouts ou exaustão de retentativas", "O Loki registraria erros
específicos, como ConnectionRefused ou GatewayTimeout"). Those are the expectations of the
method, written in the conditional, so they stay as they were. Section 4 reports what was
observed.
