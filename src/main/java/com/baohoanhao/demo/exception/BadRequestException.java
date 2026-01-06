package com.baohoanhao.demo.exception;

import org.springframework.http.HttpStatus;

/**
 * Bad Request Exception (400)
 */
public class BadRequestException extends BusinessException {
    
    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }

    public BadRequestException(String message, String errorCode) {
        super(message, HttpStatus.BAD_REQUEST, errorCode);
    }
}
