package com.failurelab.service_b.flow;

import com.failurelab.service_b.chaos.ChaosState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ProcessController {

    private static final Logger log = LoggerFactory.getLogger(ProcessController.class);

    private final ChaosState state;
    private final String serviceName;

    ProcessController(ChaosState state, @Value("${spring.application.name}") String serviceName) {
        this.state = state;
        this.serviceName = serviceName;
    }

    @GetMapping("/process")
    ProcessResponse process() throws InterruptedException {
        if (state.isLatencyActive()) {
            Thread.sleep(state.latencyMs());
        }
        log.info("Process completed with {} ms injected latency", state.latencyMs());
        return new ProcessResponse(serviceName, "OK");
    }

    record ProcessResponse(String service, String status) {
    }
}
