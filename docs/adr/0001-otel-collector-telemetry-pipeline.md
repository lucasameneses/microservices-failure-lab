# 0001 — Traces and logs go through an OpenTelemetry Collector

- **Status**: accepted
- **Context**: The paper (section 3) says OpenTelemetry is the unified standard that lets Loki and
  Jaeger consume telemetry from both services. The current code exports traces with the
  `opentelemetry-exporter-jaeger` library, which was removed from OpenTelemetry in 2023 and is
  not supported by Spring Boot 3.5, and there is no log pipeline at all.
- **Decision**: Both services export traces and logs over OTLP/HTTP to an OpenTelemetry Collector
  (`otel/opentelemetry-collector-contrib`). The Collector forwards traces to Jaeger (OTLP) and
  logs to Loki (native OTLP endpoint `/otlp`). Metrics stay as the paper describes: Micrometer
  exposes `/actuator/prometheus` and Prometheus scrapes it.
- **Alternatives**:
  - Direct export (traces to Jaeger, logs to Loki through a loki4j Logback appender): one
    container less, but each service is coupled to each backend, and it does not show OTel
    working as the unifying layer the paper claims.
  - OpenTelemetry Java agent: instruments everything automatically, but duplicates what
    Micrometer already does and hides the instrumentation from readers of the code.
- **Consequences**: One more container to run and configure. Backends can be swapped by editing
  only the Collector config. Logs carry trace and span ids, so Grafana can jump from a log line
  to its trace.
