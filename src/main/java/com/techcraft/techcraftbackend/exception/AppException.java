package com.techcraft.techcraftbackend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final Object data;

    public AppException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.data = null;
    }

    public AppException(String message, HttpStatus status, Object data) {
        super(message);
        this.status = status;
        this.data = data;
    }

    public AppException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.data = null;
    }
}
