package com.baohoanhao.demo.service;

import com.baohoanhao.demo.config.JwtProperties;
import com.baohoanhao.demo.dto.request.LoginRequest;
import com.baohoanhao.demo.dto.request.RefreshTokenRequest;
import com.baohoanhao.demo.dto.request.RegisterRequest;
import com.baohoanhao.demo.dto.response.AuthResponse;
import com.baohoanhao.demo.entity.User;
import com.baohoanhao.demo.entity.Role;
import com.baohoanhao.demo.exception.BadRequestException;
import com.baohoanhao.demo.exception.ConflictException;
import com.baohoanhao.demo.exception.UnauthorizedException;
import com.baohoanhao.demo.repository.UserRepository;
import com.baohoanhao.demo.security.JwtService;
import com.baohoanhao.demo.security.TokenStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Authentication Service
 * 
 * Xử lý:
 * - Đăng ký user mới
 * - Đăng nhập và phát hành JWT tokens
 * - Refresh token
 * - Logout (revoke tokens)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenStorageService tokenStorageService;
    private final JwtProperties jwtProperties;

    

    /**
     * Đăng ký user mới
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Validate: cần ít nhất email hoặc phone
        if (request.getEmail() == null && request.getPhone() == null) {
            throw new BadRequestException("Cần cung cấp Email hoặc Số điện thoại");
        }

        // 2. Kiểm tra trùng lặp
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email", "email", request.getEmail());
        }
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new ConflictException("Số điện thoại", "phone", request.getPhone());
        }

        // 3. Tạo user mới
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .active(true)
            .role(Role.USER)
                .build();

        user = userRepository.save(user);
        log.info("User registered: {}", user.getEmail() != null ? user.getEmail() : user.getPhone());

        // 4. Generate tokens và trả về
        return generateAuthResponse(user);
    }

    /**
     * Đăng nhập
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // 1. Tìm user bằng email hoặc phone
        User user = userRepository.findByIdentifier(request.getIdentifier())
                .orElseThrow(() -> new UnauthorizedException("Tài khoản hoặc mật khẩu không đúng"));

        // 2. Kiểm tra tài khoản active
        if (!user.isActive()) {
            throw new UnauthorizedException("Tài khoản đã bị vô hiệu hóa");
        }

        // 3. Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Tài khoản hoặc mật khẩu không đúng");
        }

        log.info("User logged in: {}", request.getIdentifier());

        // 4. Generate tokens
        return generateAuthResponse(user);
    }

    /**
     * Refresh access token
     */
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // 1. Validate refresh token
        if (!jwtService.isTokenValid(refreshToken)) {
            throw new UnauthorizedException("Refresh token không hợp lệ");
        }

        // 2. Kiểm tra token type
        if (!"refresh".equals(jwtService.extractTokenType(refreshToken))) {
            throw new UnauthorizedException("Token không phải refresh token");
        }

        // 3. Extract user ID
        String userId = jwtService.extractUserId(refreshToken);

        // 4. Kiểm tra refresh token có trong Redis không (chống token bị đánh cắp)
        String storedToken = tokenStorageService.getRefreshToken(userId);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new UnauthorizedException("Refresh token đã bị thu hồi");
        }

        // 5. Tìm user
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new UnauthorizedException("User không tồn tại"));

        // 6. Xóa refresh token cũ
        tokenStorageService.deleteRefreshToken(userId);

        log.info("Token refreshed for user: {}", user.getId());

        // 7. Generate tokens mới
        return generateAuthResponse(user);
    }

    /**
     * Logout - Revoke tokens
     */
    public void logout(String accessToken, String userId) {
        // 1. Blacklist access token (để nó không dùng được nữa dù chưa hết hạn)
        if (accessToken != null && jwtService.isTokenValid(accessToken)) {
            long ttl = jwtService.getTimeToLive(accessToken);
            tokenStorageService.blacklistToken(accessToken, ttl);
        }

        // 2. Xóa refresh token
        tokenStorageService.deleteRefreshToken(userId);

        log.info("User logged out: {}", userId);
    }

    /**
     * Logout tất cả devices
     */
    public void logoutAll(String userId) {
        tokenStorageService.revokeAllUserTokens(userId);
        log.info("User logged out from all devices: {}", userId);
    }

    /**
     * Lấy thông tin user hiện tại (id, email, fullName, phone, role, authorities)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCurrentUserProfile(String userId, Collection<?> authorities) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new UnauthorizedException("User không tồn tại"));

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId().toString());
        profile.put("email", user.getEmail());
        profile.put("fullName", user.getFullName());
        profile.put("phone", user.getPhone());
        profile.put("role", user.getRole().name());
        profile.put("authorities", authorities);

        return profile;
    }

    // ==================== Private Methods ====================

    /**
     * Generate AuthResponse với access và refresh tokens
     */
    private AuthResponse generateAuthResponse(User user) {
        // 1. Generate tokens
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        // 2. Lưu refresh token vào Redis
        tokenStorageService.storeRefreshToken(
                user.getId().toString(),
                refreshToken,
                jwtProperties.getRefreshTokenExpiration()
        );

        // 3. Build response
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpiration() / 1000) // Convert to seconds
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId().toString())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .fullName(user.getFullName())
                        .role(user.getRole().name())
                        .build())
                .build();
    }
}