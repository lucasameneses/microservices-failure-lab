package com.failurelab.service_a.chaos;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

@Component
public class ChaosState {

    private final AtomicBoolean cycle = new AtomicBoolean();

    public boolean isCycleActive() {
        return cycle.get();
    }

    void activateCycle() {
        cycle.set(true);
    }

    void reset() {
        cycle.set(false);
    }
}
