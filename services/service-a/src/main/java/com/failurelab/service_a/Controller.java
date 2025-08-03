package com.failurelab.service_a;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@RestController
public class Controller {

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private MeterRegistry registry;

    @GetMapping("/hello")
    public String hello() {
        return "Hello from Service A!";
    }

    @GetMapping("/call-b")
    public String callServiceB() {
        registry.counter("service_a.calls", "target", "service_b").increment();

        long start = System.currentTimeMillis();
        String response = restTemplate.getForObject("http://service-b:8081/hello", String.class);
        long duration = System.currentTimeMillis() - start;

        registry.timer("service_a.request_duration", "target", "service_b").record(duration, TimeUnit.MILLISECONDS);
        return "Service A called Service B: " + response;
    }
}
