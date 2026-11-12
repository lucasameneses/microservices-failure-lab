# Credibility checklist

Every claim the approved paper (sections 3 and 4) and the defense slides (slides 17–26 of the deck
presented in 2025) make about the lab and its tools, checked by
running the lab on 2026-11-11.

How to read it:
- Claims are quoted in Portuguese, as in the approved version.
- **Status** is `confirmed` (with evidence) or `corrected`: the lab did not confirm it, and the
  revised [`paper.pdf`](paper.pdf) now states what was observed (see
  [`divergences.md`](divergences.md)). The published [`slides.pdf`](slides.pdf) carries the
  corrected wording.
- Evidence is a screenshot in [`img/`](img/).

## Environment

| Id | Source | Claim | Scenario | Tool | Status | Evidence |
|---|---|---|---|---|---|---|
| C01 | §3, slide 16 | "O ambiente experimental consistiu em dois microsserviços interconectados" | all | — | confirmed | [latency-jaeger](img/latency-jaeger.png) |
| C02 | §3 | OTel "atuou como um padrão unificado para a geração de telemetria, incluindo métricas, logs e, principalmente, rastreamentos distribuídos" e viabilizou "que ferramentas como Loki e Jaeger pudessem consumir e correlacionar dados de diferentes microsserviços" | all | OTel Collector, Loki, Jaeger | corrected | [cycle-loki](img/cycle-loki.png), [cycle-jaeger](img/cycle-jaeger.png) |

## Scenario 1 — High latency

| Id | Source | Claim | Scenario | Tool | Status | Evidence |
|---|---|---|---|---|---|---|
| C03 | §3 item 1, slide 18 | "O Microsserviço A [...] realizou uma chamada HTTP para o Microsserviço B [...]. No Microsserviço B, foi introduzido intencionalmente um delay artificial" | latency | — | confirmed | [latency-jaeger](img/latency-jaeger.png) |
| C04 | §4.1, slide 23 | "O Prometheus capturou esse aumento de latência através de métricas de tempo de resposta." | latency | Prometheus | confirmed | [latency-grafana](img/latency-grafana.png) (Prometheus p95 2.13 s) |
| C05 | §4.1, slide 23, Fig. 11 | "Os dashboards do Grafana evidenciaram a degradação do desempenho por meio de gráficos de séries temporais durante o período da simulação." | latency | Grafana | confirmed | [latency-grafana](img/latency-grafana.png) |
| C06 | §4.1, slide 23, Figs. 9–10 | "Os traçados distribuídos do Jaeger foram cruciais para identificar o ponto exato onde o atraso foi introduzido (o serviço B)" | latency | Jaeger | confirmed | [latency-jaeger](img/latency-jaeger.png) |

## Scenario 2 — Cyclic dependency

| Id | Source | Claim | Scenario | Tool | Status | Evidence |
|---|---|---|---|---|---|---|
| C07 | §3 item 2, slide 19 | "o Microsserviço A foi configurado para chamar o Microsserviço B, que, por sua vez, realizou uma nova chamada de volta ao Microsserviço A. Um limite de chamadas foi configurado para controlar o ciclo" | cycle | — | confirmed | [cycle-jaeger](img/cycle-jaeger.png) |
| C08 | §4.2, slide 24, Figs. 12–13 | "O rastreamento distribuído do Jaeger foi fundamental para evidenciar o loop de requisições." | cycle | Jaeger | confirmed | [cycle-jaeger](img/cycle-jaeger.png) |
| C09 | §4.2, slide 24 | "Os logs centralizados no Loki registraram eventos de timeout e tentativas de retentativas excedidas" | cycle | Loki | corrected | [cycle-loki](img/cycle-loki.png) |
| C10 | §3 item 2 | "O Prometheus seria configurado para monitorar um aumento anormal nas requisições" | cycle | Prometheus | confirmed | [cycle-grafana](img/cycle-grafana.png) (`/cycle` 25 req/s vs `/flow` 5 req/s) |

## Scenario 3 — Resource exhaustion

| Id | Source | Claim | Scenario | Tool | Status | Evidence |
|---|---|---|---|---|---|---|
| C11 | §3 item 3, slide 20 | "Um endpoint específico foi implementado no Microsserviço A para simular o esgotamento de recursos através da execução de operações intensivas que consumiam grandes quantidades de memória e CPU" | resources | — | confirmed | [resources-grafana](img/resources-grafana.png) |
| C12 | §4.3, slide 25 | "As métricas coletadas pelo Prometheus evidenciaram o aumento progressivo desses recursos." | resources | Prometheus | confirmed | [resources-grafana](img/resources-grafana.png) (CPU 100 %, heap 246 MB) |
| C13 | §4.3, slide 25, Fig. 14 | "Os dashboards do Grafana ilustraram essa tendência de forma clara." | resources | Grafana | confirmed | [resources-grafana](img/resources-grafana.png) |
| C14 | §4.3, slide 25 | "o Loki registrou mensagens de erro, como OutOfMemoryError, que confirmaram o impacto da simulação sobre a estabilidade do serviço" | resources | Loki | confirmed | [resources-loki](img/resources-loki.png) |

## Scenario 4 — Network communication failures

| Id | Source | Claim | Scenario | Tool | Status | Evidence |
|---|---|---|---|---|---|---|
| C15 | §3 item 4, §4.4, slide 21 | "introdução de atrasos e perda de pacotes (usando 'tc' no Linux) e pela desativação temporária do Microsserviço B" | network-tc, network-down | — | confirmed | [network-grafana](img/network-grafana.png) |
| C16 | §4.4, slide 26, Fig. 15 | "Um aumento nos códigos de erro HTTP foi capturado pelo Prometheus." | network-tc, network-down | Prometheus, Grafana | confirmed | [network-grafana](img/network-grafana.png) (504 with tc, 502 with service-b down) |
| C17 | §4.4, slide 26, Fig. 16 | "Os rastreamentos do Jaeger evidenciaram a interrupção no fluxo de requisições entre os serviços." | network-tc, network-down | Jaeger | confirmed | [network-jaeger](img/network-jaeger.png), [network-down-jaeger](img/network-down-jaeger.png) |
| C18 | §4.4, slide 26 | "Os logs registrados pelo Loki complementaram a análise, destacando erros como ConnectionRefused e GatewayTimeout." | network-tc, network-down | Loki | corrected | [network-loki](img/network-loki.png) |
