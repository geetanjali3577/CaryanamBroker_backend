package com.caryanam.caryanam_broker.exception;


import org.springframework.http.HttpStatus;

public class BadRequestException extends RuntimeException {

    private final int status;

    public BadRequestException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST.value();
    }

    public int getStatus() {
        return status;
    }
}
