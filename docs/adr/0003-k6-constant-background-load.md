# 0003 — A k6 container generates constant background traffic

- **Status**: accepted
- **Context**: Grafana time-series panels only show a degradation if there is traffic before,
  during and after a scenario. The author already used k6 in previous research.
- **Decision**: A `k6` service in the Compose file (profile `load`) sends a low, constant request
  rate to the normal A → B flow for as long as the lab is up.
- **Alternatives**:
  - A `curl` loop inside the scenario scripts: simpler, but irregular and it stops when the
    script ends, so there is no baseline.
- **Consequences**: Dashboards always have a baseline. The load is deliberately low; this is not
  a benchmark (out of scope).
