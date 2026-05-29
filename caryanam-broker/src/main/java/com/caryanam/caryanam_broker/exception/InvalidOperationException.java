package com.caryanam.caryanam_broker.exception;

import org.springframework.http.HttpStatus;

public class InvalidOperationException extends RuntimeException {

    private final HttpStatus status;

    public InvalidOperationException(String message) {
        super(message);
        this.status = HttpStatus.UNPROCESSABLE_ENTITY;
    }

    public HttpStatus getStatus() {
        return status;
    }
}