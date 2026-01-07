package com.baohoanhao.demo.controller;

import com.baohoanhao.demo.dto.response.ApiResponse;
import com.baohoanhao.demo.dto.response.AuthResponse;
import com.baohoanhao.demo.service.Oauth2LoginService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

/**
 * OAuth2 callback controller (stateless, trả JWT nội bộ).
 */
@RestController
@RequestMapping("/api/auth/oauth2")
@RequiredArgsConstructor
@Profile("oauth")
@ConditionalOnProperty(value = "app.oauth.enabled", havingValue = "true")
@ConditionalOnBean(Oauth2LoginService.class)
public class OAuthController {

    private final Oauth2LoginService oauth2LoginService;

    @GetMapping("/callback/{provider}")
    public ResponseEntity<ApiResponse<AuthResponse>> handleCallback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam(required = false) String state,
            HttpServletRequest request
    ) {
        String redirectUri = request.getRequestURL().toString();
        AuthResponse authResponse = oauth2LoginService.handleCallback(provider, code, redirectUri);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập OAuth thành công", authResponse));
    }
}
