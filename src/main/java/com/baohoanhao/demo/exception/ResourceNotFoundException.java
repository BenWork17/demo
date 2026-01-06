package com.baohoanhao.demo.exception;

import org.springframework.http.HttpStatus;

/**
 * Resource Not Found Exception (404)
 */
public class ResourceNotFoundException extends BusinessException {
    
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s không tìm thấy với %s: '%s'", resourceName, fieldName, fieldValue),
                HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
}
