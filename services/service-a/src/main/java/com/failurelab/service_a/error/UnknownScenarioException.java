package com.failurelab.service_a.error;

import org.springframework.http.HttpStatus;

public class UnknownScenarioException extends ApiException {

    public UnknownScenarioException(String scenario) {
        super(HttpStatus.NOT_FOUND, "UNKNOWN_SCENARIO", "Unknown scenario: " + scenario, null);
    }
}
