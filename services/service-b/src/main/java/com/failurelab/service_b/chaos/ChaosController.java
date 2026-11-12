package com.failurelab.service_b.chaos;

import com.failurelab.service_b.error.InvalidParameterException;
import com.failurelab.service_b.error.UnknownScenarioException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chaos")
class ChaosController {

    private static final Logger log = LoggerFactory.getLogger(ChaosController.class);
    private static final String LATENCY = "latency";
    private static final int DEFAULT_DELAY_MS = 2000;
    private static final int MAX_DELAY_MS = 10000;

    private final ChaosState state;
    private final String serviceName;

    ChaosController(ChaosState state, @Value("${spring.application.name}") String serviceName) {
        this.state = state;
        this.serviceName = serviceName;
    }

    @GetMapping
    ChaosView status() {
        return new ChaosView(serviceName, Map.of(LATENCY, state.isLatencyActive()), state.latencyMs());
    }

    @PostMapping("/{scenario}")
    ChaosView activate(@PathVariable String scenario, @RequestBody(required = false) LatencyRequest request) {
        if (!LATENCY.equals(scenario)) {
            throw new UnknownScenarioException(scenario);
        }
        int delayMs = delayFrom(request);
        state.activateLatency(delayMs);
        log.warn("Scenario latency activated: {} ms delay on /process", delayMs);
        return status();
    }

    @DeleteMapping
    ChaosView reset() {
        state.reset();
        log.info("All scenarios reset");
        return status();
    }

    private static int delayFrom(LatencyRequest request) {
        if (request == null || request.delayMs() == null) {
            return DEFAULT_DELAY_MS;
        }
        int delayMs = request.delayMs();
        if (delayMs < 1 || delayMs > MAX_DELAY_MS) {
            throw new InvalidParameterException("delayMs must be between 1 and " + MAX_DELAY_MS);
        }
        return delayMs;
    }

    record LatencyRequest(Integer delayMs) {
    }

    record ChaosView(String service, Map<String, Boolean> active, int delayMs) {
    }
}
