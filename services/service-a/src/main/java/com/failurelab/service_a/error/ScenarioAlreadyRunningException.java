package com.failurelab.service_a.error;

import org.springframework.http.HttpStatus;

public class ScenarioAlreadyRunningException extends ApiException {

    public ScenarioAlreadyRunningException(String scenario) {
        super(HttpStatus.CONFLICT, "SCENARIO_ALREADY_RUNNING", "Scenario already running: " + scenario, null);
    }
}
