package com.failurelab.service_b;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicBoolean;

@RestController
public class Controller {

    private String failureMode = "none";
    private int latencyMs = 0;
    private final AtomicBoolean cpuExhaustionActive = new AtomicBoolean(false);
    private final AtomicBoolean networkFailureActive = new AtomicBoolean(false);


    @GetMapping("/hello")
    public String hello() {
        return "Hello from Service B!";
    }

    // Endpoint para configurar falhas
    @PostMapping("/actuator/failure")
    public ResponseEntity<String> configureFailure(@RequestBody FailureConfig config) {
        this.failureMode = config.getType();

        switch(failureMode) {
            case "latency":
                this.latencyMs = config.getValue();
                break;
            case "cpu_exhaustion":
                this.cpuExhaustionActive.set(true);
                break;
            case "network_failure":
                this.networkFailureActive.set(true);
                break;
            case "cycle":
                // Será implementado posteriormente
                break;
        }
        return ResponseEntity.ok("Failure mode set: " + failureMode);
    }

    // Endpoint para resetar falhas
    @PostMapping("/actuator/reset")
    public ResponseEntity<String> resetFailure() {
        this.failureMode = "none";
        this.latencyMs = 0;
        this.cpuExhaustionActive.set(false);
        this.networkFailureActive.set(false);
        return ResponseEntity.ok("Failure modes reset");
    }

    // Endpoint principal com simulação de falhas
    @GetMapping("/api")
    public ResponseEntity<String> apiEndpoint() {
        // Simular falha de rede
        if (networkFailureActive.get()) {
            return ResponseEntity.status(503).body("Network failure simulated");
        }

        // Simular latência
        if (latencyMs > 0) {
            try {
                Thread.sleep(latencyMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Simular esgotamento de CPU
        if (cpuExhaustionActive.get()) {
            simulateCpuExhaustion();
        }

        return ResponseEntity.ok("OK from Service B");
    }

    private void simulateCpuExhaustion() {
        long endTime = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < endTime) {
            // Loop intensivo de CPU
            double result = 0;
            for (int i = 0; i < 1000000; i++) {
                result += Math.sin(i) * Math.cos(i);
            }
        }
    }

    static class FailureConfig {
        private String type;
        private int value;

        // Getters e setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
    }
}
