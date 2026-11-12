package com.failurelab.service_a.flow;

import com.failurelab.service_a.chaos.ChaosState;
import com.failurelab.service_a.client.ServiceBClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class FlowController {

    private static final Logger log = LoggerFactory.getLogger(FlowController.class);

    private final ServiceBClient serviceB;
    private final ChaosState state;
    private final String serviceName;

    FlowController(ServiceBClient serviceB, ChaosState state,
                   @Value("${spring.application.name}") String serviceName) {
        this.serviceB = serviceB;
        this.state = state;
        this.serviceName = serviceName;
    }

    @GetMapping("/flow")
    FlowResponse flow() {
        boolean cycle = state.isCycleActive();
        JsonNode downstream = cycle ? serviceB.cycle(1) : serviceB.process();
        log.info("Flow completed via service-b {}", cycle ? "/cycle" : "/process");
        return new FlowResponse(serviceName, downstream);
    }

    record FlowResponse(String service, JsonNode downstream) {
    }
}
