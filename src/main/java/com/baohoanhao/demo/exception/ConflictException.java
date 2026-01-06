package com.baohoanhao.demo.exception;

import org.springframework.http.HttpStatus;

/**
 * Conflict Exception (409) - Dùng cho duplicate resource
 */
public class ConflictException extends BusinessException {
    
    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT, "CONFLICT");
    }

    public ConflictException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s đã tồn tại với %s: '%s'", resourceName, fieldName, fieldValue),
                HttpStatus.CONFLICT, "DUPLICATE_RESOURCE");
    }
}
