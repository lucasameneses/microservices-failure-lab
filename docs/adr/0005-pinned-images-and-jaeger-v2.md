# 0005 — Pinned image versions and Jaeger v2

- **Status**: accepted
- **Context**: Every image used `latest`, so the lab could break or change behavior between
  clones. The Jaeger 1.x line reached end of life in December 2025.
- **Decision**: Every image is pinned to an exact version. Jaeger uses the v2 image
  (`jaegertracing/jaeger`), which receives OTLP and has the same UI used in the paper's figures.
  Services are built with a multi-stage Dockerfile into a jar and run on a JRE image, instead of
  running `mvnw spring-boot:run` at container start.
- **Alternatives**:
  - Keep `latest`: no maintenance, but not reproducible.
  - Keep Jaeger 1.x: matches the paper's screenshots exactly, but is unsupported.
- **Consequences**: Reproducible runs, faster start, explicit heap. Versions must be bumped by
  hand.
