package com.failurelab.service_b.cycle;

import com.failurelab.service_b.client.ServiceAClient;
import com.failurelab.service_b.error.InvalidParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CycleController {

    private static final Logger log = LoggerFactory.getLogger(CycleController.class);

    private final ServiceAClient peer;
    private final String serviceName;
    private final int maxDepth;

    CycleController(ServiceAClient peer,
                    @Value("${spring.application.name}") String serviceName,
                    @Value("${lab.cycle.max-depth}") int maxDepth) {
        this.peer = peer;
        this.serviceName = serviceName;
        this.maxDepth = maxDepth;
    }

    @GetMapping("/cycle")
    CycleResponse cycle(@RequestParam(defaultValue = "0") int depth) {
        if (depth < 0) {
            throw new InvalidParameterException("depth must be >= 0");
        }
        if (depth >= maxDepth) {
            log.warn("Cycle limit reached at depth {} (max {})", depth, maxDepth);
            return new CycleResponse(serviceName, depth, true, null);
        }
        log.info("Cycle hop depth={}", depth);
        return new CycleResponse(serviceName, depth, false, peer.cycle(depth + 1));
    }
}
