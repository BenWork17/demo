package com.baohoanhao.demo.controller;

import com.baohoanhao.demo.dto.request.LoginRequest;
import com.baohoanhao.demo.dto.request.RefreshTokenRequest;
import com.baohoanhao.demo.dto.request.RegisterRequest;
import com.baohoanhao.demo.dto.response.ApiResponse;
import com.baohoanhao.demo.dto.response.AuthResponse;
import com.baohoanhao.demo.exception.UnauthorizedException;
import com.baohoanhao.demo.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 * 
 * REST API Endpoints:
 * - POST /api/auth/register  - Đăng ký tài khoản mới
 * - POST /api/auth/login     - Đăng nhập
 * - POST /api/auth/refresh   - Refresh access token
 * - POST /api/auth/logout    - Đăng xuất (cần authentication)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Đăng ký tài khoản mới
     * 
     * POST /api/auth/register
     * Body: { fullName, email?, phone?, password }
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        
        AuthResponse authResponse = authService.register(request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký thành công", authResponse));
    }

    /**
     * Đăng nhập
     * 
     * POST /api/auth/login
     * Body: { identifier (email/phone), password }
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        
        AuthResponse authResponse = authService.login(request);
        
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", authResponse));
    }

    /**
     * Refresh access token
     * 
     * POST /api/auth/refresh
     * Body: { refreshToken }
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        
        AuthResponse authResponse = authService.refreshToken(request);
        
        return ResponseEntity.ok(ApiResponse.success("Token đã được làm mới", authResponse));
    }

    /**
     * Đăng xuất
     * 
     * POST /api/auth/logout
     * Header: Authorization: Bearer {accessToken}
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            Authentication authentication) {
        
        // Lấy token từ header
        String authHeader = request.getHeader("Authorization");
        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }

        // Lấy userId từ authentication (được set bởi JwtAuthenticationFilter)
        String userId = authentication != null ? authentication.getName() : null;

        if (userId != null) {
            authService.logout(accessToken, userId);
        }

        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công"));
    }

    /**
     * Đăng xuất tất cả thiết bị
     * 
     * POST /api/auth/logout-all
     * Header: Authorization: Bearer {accessToken}
     */
    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(Authentication authentication) {
        String userId = authentication.getName();
        authService.logoutAll(userId);
        
        return ResponseEntity.ok(ApiResponse.success("Đã đăng xuất khỏi tất cả thiết bị"));
    }

    /**
     * Lấy thông tin user hiện tại (test endpoint - cần auth)
     * 
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Object>> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            throw new UnauthorizedException("Vui lòng đăng nhập để thực hiện thao tác này");
        }
        String userId = authentication.getName();
        return ResponseEntity.ok(ApiResponse.success(
            "User info",
            authService.getCurrentUserProfile(userId, authentication.getAuthorities())
        ));
    }
}
