package com.baohoanhao.demo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT Configuration Properties
 * Đọc từ application.yaml prefix "jwt"
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    
    private String secretKey;
    private long accessTokenExpiration;
    private long refreshTokenExpiration;
}
