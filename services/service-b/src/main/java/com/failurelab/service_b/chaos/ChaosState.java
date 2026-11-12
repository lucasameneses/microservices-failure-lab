package com.failurelab.service_b.chaos;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class ChaosState {

    private final AtomicInteger latencyMs = new AtomicInteger();

    public int latencyMs() {
        return latencyMs.get();
    }

    public boolean isLatencyActive() {
        return latencyMs.get() > 0;
    }

    void activateLatency(int delayMs) {
        latencyMs.set(delayMs);
    }

    void reset() {
        latencyMs.set(0);
    }
}
