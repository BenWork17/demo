package com.baohoanhao.demo.exception;

import org.springframework.http.HttpStatus;

/**
 * Unauthorized Exception (401)
 */
public class UnauthorizedException extends BusinessException {
    
    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }

    public UnauthorizedException() {
        super("Vui lòng đăng nhập để tiếp tục", HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }
}
