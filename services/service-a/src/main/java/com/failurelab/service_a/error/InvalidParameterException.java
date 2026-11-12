package com.failurelab.service_a.error;

import org.springframework.http.HttpStatus;

public class InvalidParameterException extends ApiException {

    public InvalidParameterException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", message, null);
    }
}
