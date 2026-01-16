
package com.baohoanhao.demo.controller;

import com.baohoanhao.demo.dto.response.ApiResponse;
import com.baohoanhao.demo.dto.response.AuthResponse;
import com.baohoanhao.demo.service.Oauth2LoginService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

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
    @Value("${app.frontend-url}")
    private String frontendUrl;

    @GetMapping("/callback/{provider}")
    public ResponseEntity<ApiResponse<AuthResponse>> handleCallback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam(required = false) String state,
            HttpServletRequest request
    ) {
        String redirectUri = request.getRequestURL().toString();
        AuthResponse authResponse = oauth2LoginService.handleCallback(provider, code, redirectUri);

        // Build redirect to FE with tokens and basic user info
        UriComponentsBuilder targetBuilder = resolveRedirectUrl(state)
                .map(UriComponentsBuilder::fromUriString)
                .orElseGet(() -> UriComponentsBuilder.fromUriString(frontendUrl));

        String target = targetBuilder
                .queryParam("accessToken", authResponse.getAccessToken())
                .queryParam("access_token", authResponse.getAccessToken())
                .queryParam("token", authResponse.getAccessToken())
                .queryParam("refreshToken", authResponse.getRefreshToken())
                .queryParam("refresh_token", authResponse.getRefreshToken())
                .queryParam("tokenType", authResponse.getTokenType())
                .queryParam("expiresIn", authResponse.getExpiresIn())
                .queryParam("userId", authResponse.getUser().getId())
                .queryParam("email", authResponse.getUser().getEmail())
                .queryParam("fullName", authResponse.getUser().getFullName())
                .queryParam("role", authResponse.getUser().getRole())
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(target))
                .build();
    }

    private Optional<String> resolveRedirectUrl(String state) {
        if (state == null || state.isBlank()) {
            return Optional.empty();
        }
        String decoded = URLDecoder.decode(state, StandardCharsets.UTF_8);

        // Handle double-encoded state (e.g., %257B -> %7B -> {)
        if (decoded.contains("%")) {
            decoded = URLDecoder.decode(decoded, StandardCharsets.UTF_8);
        }

        // If state is a JSON like {"redirect_url":"..."}
        if (decoded.contains("\"redirect_url\":\"")) {
            int start = decoded.indexOf("\"redirect_url\":\"") + 16;
            int end = decoded.indexOf("\"", start);
            if (end > start) {
                return Optional.of(decoded.substring(start, end));
            }
        }

        // If state is a simple query string like redirect_url=...
        if (decoded.contains("redirect_url=")) {
            Map<String, String> params = UriComponentsBuilder.fromUriString("/dummy?" + decoded)
                    .build()
                    .getQueryParams()
                    .toSingleValueMap();
            String redirectUrl = params.get("redirect_url");
            if (redirectUrl != null && !redirectUrl.isBlank()) {
                return Optional.of(redirectUrl);
            }
        }

        return Optional.empty();
    }
}
