package com.techcraft.techcraftbackend.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends AppException {

    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    public BadRequestException(String message, Object data) {
        super(message, HttpStatus.BAD_REQUEST, data);
    }
}
