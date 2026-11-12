# 0002 — The OutOfMemoryError is real and caught inside service-a

- **Status**: accepted
- **Context**: The paper says Loki recorded an `OutOfMemoryError` from the resource-exhaustion
  scenario, and the lab forbids writing an error that did not happen. A JVM that dies
  on OOM prints its fatal message to stderr, outside the Logback/OTLP log pipeline.
- **Decision**: service-a runs with a small fixed heap (`-Xmx256m`). The scenario allocates memory
  in chunks on a background job until the JVM throws a real `OutOfMemoryError`. The job catches
  it, logs it at ERROR level with the exception, drops every reference so the heap can be
  collected, and ends. The service keeps answering.
- **Alternatives**:
  - Let the JVM exit (`-XX:+ExitOnOutOfMemoryError`) and have Docker restart the container:
    more dramatic, but the fatal message only reaches Loki if container logs are also collected
    (one more component), and recovery timing becomes nondeterministic.
- **Consequences**: Deterministic and repeatable. The error shown in Loki is the real exception
  object. Catching `OutOfMemoryError` is acceptable only because this is a lab: the code that
  does it lives in the scenario job alone and is documented as such.
