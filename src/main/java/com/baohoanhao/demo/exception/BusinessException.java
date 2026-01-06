package com.baohoanhao.demo.exception;

import org.springframework.http.HttpStatus;

/**
 * Base Business Exception
 * Tất cả custom exception kế thừa từ đây
 */
public abstract class BusinessException extends RuntimeException {
    
    private final HttpStatus status;
    private final String errorCode;

    protected BusinessException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
