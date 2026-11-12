# 0006 — Scenarios are runtime toggles behind a /chaos API

- **Status**: accepted
- **Context**: Scenarios must be triggered and reverted independently without recreating the
  environment. The current code exposes `POST /actuator/failure`, inside the namespace
  Spring Boot reserves for Actuator endpoints.
- **Decision**: Each service exposes `GET /chaos`, `POST /chaos/{scenario}` and `DELETE /chaos`.
  A scenario lives in the service the paper places it in: `latency` in service-b, `cycle`
  and `resource-exhaustion` in service-a. Network faults are infrastructure-level and live only
  in the lab script. The background k6 traffic always calls the normal flow; an active toggle
  changes what that flow does.
- **Alternatives**:
  - Keep `/actuator/failure`: mixes lab controls with Spring's management endpoints.
  - One endpoint per fault that performs the fault per call: no baseline vs. fault contrast in
    the dashboards, since the background traffic would never see it.
- **Consequences**: A visitor drives everything through one script; dashboards show clear
  before/during/after windows.
