package com.failurelab.service_a.chaos;

import com.failurelab.service_a.chaos.ResourceExhaustionJob.ExhaustionSettings;
import com.failurelab.service_a.error.InvalidParameterException;
import com.failurelab.service_a.error.UnknownScenarioException;
import java.util.LinkedHashMap;
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
    private static final String CYCLE = "cycle";

    private final ChaosState state;
    private final ResourceExhaustionJob exhaustionJob;
    private final String serviceName;

    ChaosController(ChaosState state, ResourceExhaustionJob exhaustionJob,
                    @Value("${spring.application.name}") String serviceName) {
        this.state = state;
        this.exhaustionJob = exhaustionJob;
        this.serviceName = serviceName;
    }

    @GetMapping
    ChaosView status() {
        Map<String, Boolean> active = new LinkedHashMap<>();
        active.put(CYCLE, state.isCycleActive());
        active.put(ResourceExhaustionJob.SCENARIO, exhaustionJob.isRunning());
        return new ChaosView(serviceName, active);
    }

    @PostMapping("/{scenario}")
    ChaosView activate(@PathVariable String scenario, @RequestBody(required = false) ExhaustionRequest request) {
        switch (scenario) {
            case CYCLE -> {
                state.activateCycle();
                log.warn("Scenario cycle activated: /flow now enters the A -> B -> A cycle");
            }
            case ResourceExhaustionJob.SCENARIO -> exhaustionJob.start(settingsFrom(request));
            default -> throw new UnknownScenarioException(scenario);
        }
        return status();
    }

    @DeleteMapping
    ChaosView reset() {
        state.reset();
        exhaustionJob.stop();
        log.info("All scenarios reset");
        return status();
    }

    private static ExhaustionSettings settingsFrom(ExhaustionRequest request) {
        ExhaustionRequest values = request == null ? new ExhaustionRequest(null, null, null) : request;
        return new ExhaustionSettings(
                within("chunkMb", values.chunkMb(), 2, 1, 64),
                within("intervalMs", values.intervalMs(), 500, 50, 5000),
                within("cpuThreads", values.cpuThreads(), 2, 1, 8));
    }

    private static int within(String name, Integer value, int fallback, int min, int max) {
        if (value == null) {
            return fallback;
        }
        if (value < min || value > max) {
            throw new InvalidParameterException(name + " must be between " + min + " and " + max);
        }
        return value;
    }

    record ExhaustionRequest(Integer chunkMb, Integer intervalMs, Integer cpuThreads) {
    }

    record ChaosView(String service, Map<String, Boolean> active) {
    }
}
