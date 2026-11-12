package com.failurelab.service_b.error;

import org.springframework.http.HttpStatus;

public class DownstreamException extends ApiException {

    private DownstreamException(HttpStatus status, String code, String message, Throwable cause) {
        super(status, code, message, cause);
    }

    public static DownstreamException timeout(String target, Throwable cause) {
        return new DownstreamException(HttpStatus.GATEWAY_TIMEOUT, "DOWNSTREAM_TIMEOUT",
                target + " did not answer in time", cause);
    }

    public static DownstreamException error(String target, Throwable cause) {
        return new DownstreamException(HttpStatus.BAD_GATEWAY, "DOWNSTREAM_ERROR",
                target + " call failed: " + cause.getMessage(), cause);
    }
}
