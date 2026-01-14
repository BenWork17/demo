package com.baohoanhao.demo.service;

import com.baohoanhao.demo.config.JwtProperties;
import com.baohoanhao.demo.dto.request.LoginRequest;
import com.baohoanhao.demo.dto.request.RefreshTokenRequest;
import com.baohoanhao.demo.dto.request.RegisterRequest;
import com.baohoanhao.demo.dto.response.AuthResponse;
import com.baohoanhao.demo.entity.Role;
import com.baohoanhao.demo.entity.User;
import com.baohoanhao.demo.exception.BadRequestException;
import com.baohoanhao.demo.exception.ConflictException;
import com.baohoanhao.demo.exception.UnauthorizedException;
import com.baohoanhao.demo.repository.UserRepository;
import com.baohoanhao.demo.security.JwtService;
import com.baohoanhao.demo.security.TokenStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenStorageService tokenStorageService;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthService authService;

    private TestFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new TestFixtures();
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(3600000L);
        when(jwtProperties.getRefreshTokenExpiration()).thenReturn(86400000L);
    }

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("should register user successfully with email")
        void register_WithEmail_Success() {
            // Arrange
            RegisterRequest request = fixtures.createRegisterRequest("test@example.com", null);
            User savedUser = fixtures.createUser("test@example.com", null);

            when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed-password");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtService.generateAccessToken(any(), anyString(), anyString())).thenReturn("access-token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");

            // Act
            AuthResponse response = authService.register(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
            verify(userRepository).save(any(User.class));
            verify(tokenStorageService).storeRefreshToken(anyString(), eq("refresh-token"), anyLong());
        }

        @Test
        @DisplayName("should register user successfully with phone")
        void register_WithPhone_Success() {
            // Arrange
            RegisterRequest request = fixtures.createRegisterRequest(null, "0912345678");
            User savedUser = fixtures.createUser(null, "0912345678");

            when(userRepository.existsByPhone(request.getPhone())).thenReturn(false);
            when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed-password");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtService.generateAccessToken(any(), any(), anyString())).thenReturn("access-token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");

            // Act
            AuthResponse response = authService.register(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getUser().getPhone()).isEqualTo("0912345678");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should throw BadRequestException when both email and phone are null")
        void register_NoEmailOrPhone_ThrowsBadRequestException() {
            // Arrange
            RegisterRequest request = fixtures.createRegisterRequest(null, null);

            // Act & Assert
            assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cần cung cấp Email hoặc Số điện thoại");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("should throw ConflictException when email already exists")
        void register_DuplicateEmail_ThrowsConflictException() {
            // Arrange
            RegisterRequest request = fixtures.createRegisterRequest("duplicate@example.com", null);
            when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class);

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("should throw ConflictException when phone already exists")
        void register_DuplicatePhone_ThrowsConflictException() {
            // Arrange
            RegisterRequest request = fixtures.createRegisterRequest(null, "0912345678");
            when(userRepository.existsByPhone("0912345678")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class);

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("should login successfully with valid credentials")
        void login_ValidCredentials_Success() {
            // Arrange
            LoginRequest request = fixtures.createLoginRequest("test@example.com", "password123");
            User user = fixtures.createUser("test@example.com", null);

            when(userRepository.findByIdentifier("test@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", user.getPasswordHash())).thenReturn(true);
            when(jwtService.generateAccessToken(any(), anyString(), anyString())).thenReturn("access-token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");

            // Act
            AuthResponse response = authService.login(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            verify(tokenStorageService).storeRefreshToken(anyString(), eq("refresh-token"), anyLong());
        }

        @Test
        @DisplayName("should throw UnauthorizedException when user not found")
        void login_UserNotFound_ThrowsUnauthorizedException() {
            // Arrange
            LoginRequest request = fixtures.createLoginRequest("notfound@example.com", "password123");
            when(userRepository.findByIdentifier("notfound@example.com")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Tài khoản hoặc mật khẩu không đúng");
        }

        @Test
        @DisplayName("should throw UnauthorizedException when account is inactive")
        void login_InactiveAccount_ThrowsUnauthorizedException() {
            // Arrange
            LoginRequest request = fixtures.createLoginRequest("test@example.com", "password123");
            User user = fixtures.createInactiveUser();

            when(userRepository.findByIdentifier("test@example.com")).thenReturn(Optional.of(user));

            // Act & Assert
            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Tài khoản đã bị vô hiệu hóa");
        }

        @Test
        @DisplayName("should throw UnauthorizedException when password is incorrect")
        void login_IncorrectPassword_ThrowsUnauthorizedException() {
            // Arrange
            LoginRequest request = fixtures.createLoginRequest("test@example.com", "wrongpassword");
            User user = fixtures.createUser("test@example.com", null);

            when(userRepository.findByIdentifier("test@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongpassword", user.getPasswordHash())).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Tài khoản hoặc mật khẩu không đúng");
        }
    }

    @Nested
    @DisplayName("refreshToken()")
    class RefreshTokenTests {

        @Test
        @DisplayName("should refresh token successfully")
        void refreshToken_ValidToken_Success() {
            // Arrange
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("valid-refresh-token");
            User user = fixtures.createUser("test@example.com", null);
            String userId = user.getId().toString();

            when(jwtService.isTokenValid("valid-refresh-token")).thenReturn(true);
            when(jwtService.extractTokenType("valid-refresh-token")).thenReturn("refresh");
            when(jwtService.extractUserId("valid-refresh-token")).thenReturn(userId);
            when(tokenStorageService.getRefreshToken(userId)).thenReturn("valid-refresh-token");
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(jwtService.generateAccessToken(any(), anyString(), anyString())).thenReturn("new-access-token");
            when(jwtService.generateRefreshToken(any())).thenReturn("new-refresh-token");

            // Act
            AuthResponse response = authService.refreshToken(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("new-access-token");
            assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
            verify(tokenStorageService).deleteRefreshToken(userId);
            verify(tokenStorageService).storeRefreshToken(eq(userId), eq("new-refresh-token"), anyLong());
        }

        @Test
        @DisplayName("should throw UnauthorizedException when token is invalid")
        void refreshToken_InvalidToken_ThrowsUnauthorizedException() {
            // Arrange
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("invalid-token");

            when(jwtService.isTokenValid("invalid-token")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Refresh token không hợp lệ");
        }

        @Test
        @DisplayName("should throw UnauthorizedException when token type is not refresh")
        void refreshToken_WrongTokenType_ThrowsUnauthorizedException() {
            // Arrange
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("access-token");

            when(jwtService.isTokenValid("access-token")).thenReturn(true);
            when(jwtService.extractTokenType("access-token")).thenReturn("access");

            // Act & Assert
            assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Token không phải refresh token");
        }

        @Test
        @DisplayName("should throw UnauthorizedException when token is revoked")
        void refreshToken_RevokedToken_ThrowsUnauthorizedException() {
            // Arrange
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("revoked-token");
            String userId = UUID.randomUUID().toString();

            when(jwtService.isTokenValid("revoked-token")).thenReturn(true);
            when(jwtService.extractTokenType("revoked-token")).thenReturn("refresh");
            when(jwtService.extractUserId("revoked-token")).thenReturn(userId);
            when(tokenStorageService.getRefreshToken(userId)).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Refresh token đã bị thu hồi");
        }
    }

    @Nested
    @DisplayName("logout()")
    class LogoutTests {

        @Test
        @DisplayName("should logout successfully and blacklist token")
        void logout_ValidToken_Success() {
            // Arrange
            String accessToken = "valid-access-token";
            String userId = UUID.randomUUID().toString();

            when(jwtService.isTokenValid(accessToken)).thenReturn(true);
            when(jwtService.getTimeToLive(accessToken)).thenReturn(3600L);

            // Act
            authService.logout(accessToken, userId);

            // Assert
            verify(tokenStorageService).blacklistToken(accessToken, 3600L);
            verify(tokenStorageService).deleteRefreshToken(userId);
        }

        @Test
        @DisplayName("should handle null access token gracefully")
        void logout_NullToken_HandlesGracefully() {
            // Arrange
            String userId = UUID.randomUUID().toString();

            // Act
            authService.logout(null, userId);

            // Assert
            verify(tokenStorageService, never()).blacklistToken(anyString(), anyLong());
            verify(tokenStorageService).deleteRefreshToken(userId);
        }
    }

    @Nested
    @DisplayName("logoutAll()")
    class LogoutAllTests {

        @Test
        @DisplayName("should revoke all user tokens")
        void logoutAll_Success() {
            // Arrange
            String userId = UUID.randomUUID().toString();

            // Act
            authService.logoutAll(userId);

            // Assert
            verify(tokenStorageService).revokeAllUserTokens(userId);
        }
    }

    @Nested
    @DisplayName("getCurrentUserProfile()")
    class GetCurrentUserProfileTests {

        @Test
        @DisplayName("should return user profile successfully")
        void getCurrentUserProfile_ValidUserId_Success() {
            // Arrange
            User user = fixtures.createUser("test@example.com", "0912345678");
            Collection<String> authorities = Arrays.asList("ROLE_USER");

            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

            // Act
            Map<String, Object> profile = authService.getCurrentUserProfile(user.getId().toString(), authorities);

            // Assert
            assertThat(profile).isNotNull();
            assertThat(profile.get("id")).isEqualTo(user.getId().toString());
            assertThat(profile.get("email")).isEqualTo("test@example.com");
            assertThat(profile.get("phone")).isEqualTo("0912345678");
            assertThat(profile.get("fullName")).isEqualTo(user.getFullName());
            assertThat(profile.get("role")).isEqualTo("USER");
            assertThat(profile.get("authorities")).isEqualTo(authorities);
        }

        @Test
        @DisplayName("should throw UnauthorizedException when userId format is invalid")
        void getCurrentUserProfile_InvalidUuidFormat_ThrowsUnauthorizedException() {
            // Arrange
            String invalidUserId = "invalid-uuid";
            Collection<String> authorities = Arrays.asList("ROLE_USER");

            // Act & Assert
            assertThatThrownBy(() -> authService.getCurrentUserProfile(invalidUserId, authorities))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Định dạng User ID không hợp lệ");
        }

        @Test
        @DisplayName("should throw UnauthorizedException when user not found")
        void getCurrentUserProfile_UserNotFound_ThrowsUnauthorizedException() {
            // Arrange
            UUID userId = UUID.randomUUID();
            Collection<String> authorities = Arrays.asList("ROLE_USER");

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.getCurrentUserProfile(userId.toString(), authorities))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User không tồn tại");
        }
    }

    static class TestFixtures {

        RegisterRequest createRegisterRequest(String email, String phone) {
            RegisterRequest request = new RegisterRequest();
            request.setEmail(email);
            request.setPhone(phone);
            request.setFullName("Test User");
            request.setPassword("password123");
            return request;
        }

        LoginRequest createLoginRequest(String identifier, String password) {
            LoginRequest request = new LoginRequest();
            request.setIdentifier(identifier);
            request.setPassword(password);
            return request;
        }

        User createUser(String email, String phone) {
            return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .phone(phone)
                .fullName("Test User")
                .passwordHash("$2a$10$hashedPassword")
                .role(Role.USER)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        }

        User createInactiveUser() {
            return User.builder()
                .id(UUID.randomUUID())
                .email("inactive@example.com")
                .phone("0912345678")
                .fullName("Inactive User")
                .passwordHash("$2a$10$hashedPassword")
                .role(Role.USER)
                .active(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        }
    }
}
