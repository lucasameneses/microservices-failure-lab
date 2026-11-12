# Docs

This lab is the experimental base of my undergraduate thesis (TCC), presented in December 2025
at the Departamento de Telemática (DTEL) of the Instituto Federal do Ceará (IFCE):

> **Análise de Cenários de Falha em Microsserviços: Uma Abordagem Baseada em Observabilidade**
> Lucas Alves Meneses, advised by Cidcley Teixeira de Souza.

The paper describes four failure scenarios (high latency, cyclic dependency, resource exhaustion
and network failures) and what Prometheus, Grafana, Jaeger and Loki show in each one. This
repository rebuilds that environment so anyone can run the scenarios and check the claims.

## Files

| File | What it is |
|---|---|
| [`paper.pdf`](paper.pdf) | The paper, in Portuguese. Revised version, see below. |
| [`slides.pdf`](slides.pdf) | The presentation, rebuilt in Beamer from the revised paper with screenshots from this lab. Slides 6–12 cover the lab. |
| [`credibility-checklist.md`](credibility-checklist.md) | Every claim of the paper and slides about the tools, checked against the running lab, with a screenshot. |
| [`divergences.md`](divergences.md) | The three claims the lab did not confirm, and how the paper now states them. |
| [`architecture.md`](architecture.md), [`adr/`](adr/) | How the lab is built and why. |
| [`failure-lab-api.md`](failure-lab-api.md) | The HTTP calls that drive the lab. |
| [`img/`](img/) | Screenshots used as evidence. |

## About the revised paper and slides

`paper.pdf` is the approved text with these changes and nothing else:

- Three broken figure references in sections 3 and 4 were fixed.
- The sentences about the tools that the rebuilt lab did not confirm were corrected (claims C02,
  C09 and C18 in [`divergences.md`](divergences.md)). The lab showed something different from
  what the text said, and the text was changed to match the evidence, never the other way round.

`slides.pdf` is not the deck shown at the defense. It was rebuilt from the revised paper, so it
already carries the corrected wording, and its result slides use screenshots from this lab. The
slide numbers quoted in the checklist and the divergences refer to the defense deck.
