package com.baohoanhao.demo.controller;

import com.baohoanhao.demo.dto.request.LoginRequest;
import com.baohoanhao.demo.dto.response.AuthResponse;
import com.baohoanhao.demo.exception.UnauthorizedException;
import com.baohoanhao.demo.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class cho AuthController với focus vào Login endpoint
 *
 * Áp dụng các phương pháp:
 * - Kiểm thử giá trị biên (Boundary Value Testing)
 * - Kiểm thử giá trị đặc biệt (Special Value Testing)
 *
 * Test Coverage:
 * 1. Nhập sai email
 * 2. Để trống password
 * 3. Login thành công
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AuthController - Login Endpoint Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private AuthResponse mockAuthResponse;

    @BeforeEach
    void setUp() {
        // Setup mock AuthResponse
        mockAuthResponse = AuthResponse.builder()
                .accessToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
                .refreshToken("refresh_token_12345")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .user(AuthResponse.UserInfo.builder()
                        .id("550e8400-e29b-41d4-a716-446655440000")
                        .email("test@example.com")
                        .fullName("Test User")
                        .role("USER")
                        .build())
                .build();
    }

    @Nested
    @DisplayName("1. Kiểm thử giá trị biên - Email/Identifier")
    class BoundaryValueTests_Email {

        @Test
        @DisplayName("TC-BV-001: Email rỗng (empty string) - Phải trả về 400 Bad Request")
        void testLogin_EmptyEmail_ShouldReturnBadRequest() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("");
            request.setPassword("ValidPass123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Vui lòng nhập Email hoặc Số điện thoại"));

            verify(authService, never()).login(any());
        }

        @Test
        @DisplayName("TC-BV-002: Email chỉ có khoảng trắng - Phải trả về 400 Bad Request")
        void testLogin_WhitespaceEmail_ShouldReturnBadRequest() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("   ");
            request.setPassword("ValidPass123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Vui lòng nhập Email hoặc Số điện thoại"));

            verify(authService, never()).login(any());
        }

        @Test
        @DisplayName("TC-BV-003: Email có 1 ký tự (minimum length) - Phải gọi service")
        void testLogin_SingleCharacterEmail_ShouldCallService() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("a");
            request.setPassword("ValidPass123");

            when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Đăng nhập thành công"));

            verify(authService, times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("TC-BV-004: Email rất dài (maximum practical length) - Phải gọi service")
        void testLogin_VeryLongEmail_ShouldCallService() throws Exception {
            String longEmail = "a".repeat(255) + "@example.com";
            LoginRequest request = new LoginRequest();
            request.setIdentifier(longEmail);
            request.setPassword("ValidPass123");

            when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(authService, times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("TC-BV-005: Email null - Phải trả về 400 Bad Request")
        void testLogin_NullEmail_ShouldReturnBadRequest() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier(null);
            request.setPassword("ValidPass123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any());
        }
    }

    @Nested
    @DisplayName("2. Kiểm thử giá trị biên - Password")
    class BoundaryValueTests_Password {

        @Test
        @DisplayName("TC-BV-006: Password rỗng (empty string) - Phải trả về 400 Bad Request")
        void testLogin_EmptyPassword_ShouldReturnBadRequest() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("test@example.com");
            request.setPassword("");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Vui lòng nhập mật khẩu"));

            verify(authService, never()).login(any());
        }

        @Test
        @DisplayName("TC-BV-007: Password chỉ có khoảng trắng - Phải gọi service (backend accept)")
        void testLogin_WhitespacePassword_ShouldCallService() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("test@example.com");
            request.setPassword("      ");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new UnauthorizedException("Tài khoản hoặc mật khẩu không đúng"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());

            verify(authService, times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("TC-BV-008: Password có 1 ký tự (minimum length) - Phải gọi service")
        void testLogin_SingleCharPassword_ShouldCallService() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("test@example.com");
            request.setPassword("1");

            when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(authService, times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("TC-BV-009: Password rất dài - Phải gọi service")
        void testLogin_VeryLongPassword_ShouldCallService() throws Exception {
            String longPassword = "a".repeat(1000);
            LoginRequest request = new LoginRequest();
            request.setIdentifier("test@example.com");
            request.setPassword(longPassword);

            when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(authService, times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("TC-BV-010: Password null - Phải trả về 400 Bad Request")
        void testLogin_NullPassword_ShouldReturnBadRequest() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("test@example.com");
            request.setPassword(null);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any());
        }
    }

    @Nested
    @DisplayName("3. Kiểm thử giá trị đặc biệt - Email Format")
    class SpecialValueTests_EmailFormat {

        @Test
        @DisplayName("TC-SV-001: Email không có @ (invalid format) - Backend xử lý như username")
        void testLogin_EmailWithoutAt_ShouldCallService() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("invalidemail.com");
            request.setPassword("ValidPass123");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new UnauthorizedException("Tài khoản hoặc mật khẩu không đúng"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Tài khoản hoặc mật khẩu không đúng"));

            verify(authService, times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("TC-SV-002: Email có nhiều @ (special characters)")
        void testLogin_EmailWithMultipleAt_ShouldCallService() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("test@@example.com");
            request.setPassword("ValidPass123");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new UnauthorizedException("Tài khoản hoặc mật khẩu không đúng"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());

            verify(authService, times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("TC-SV-003: Email có ký tự đặc biệt hợp lệ (+, ., -)")
        void testLogin_EmailWithSpecialChars_ShouldSucceed() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("test+tag@example.com");
            request.setPassword("ValidPass123");

            when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").exists());

            verify(authService, times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("TC-SV-004: Số điện thoại Việt Nam hợp lệ (0901234567)")
        void testLogin_ValidVietnamesePhone_ShouldSucceed() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("0901234567");
            request.setPassword("ValidPass123");

            AuthResponse phoneAuthResponse = AuthResponse.builder()
                    .accessToken("token")
                    .refreshToken("refresh")
                    .tokenType("Bearer")
                    .expiresIn(3600L)
                    .user(AuthResponse.UserInfo.builder()
                            .id("550e8400-e29b-41d4-a716-446655440000")
                            .phone("0901234567")
                            .fullName("Test User")
                            .role("USER")
                            .build())
                    .build();

            when(authService.login(any(LoginRequest.class))).thenReturn(phoneAuthResponse);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.user.phone").value("0901234567"));

            verify(authService, times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("TC-SV-005: Email với Unicode/tiếng Việt")
        void testLogin_EmailWithUnicode_ShouldCallService() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("nguyễn@example.com");
            request.setPassword("ValidPass123");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new UnauthorizedException("Tài khoản hoặc mật khẩu không đúng"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());

            verify(authService, times(1)).login(any(LoginRequest.class));
        }
    }

    @Nested
    @DisplayName("4. Kiểm thử giá trị đặc biệt - Password Format")
    class SpecialValueTests_PasswordFormat {

        @Test
        @DisplayName("TC-SV-006: Password có ký tự đặc biệt (!@#$%^&*())")
        void testLogin_PasswordWithSpecialChars_ShouldSucceed() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("test@example.com");
            request.setPassword("!@#$%^&*()");

            when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(authService, times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("TC-SV-007: Password có Unicode/emoji")
        void testLogin_PasswordWithUnicode_ShouldCallService() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("test@example.com");
            request.setPassword("Pass🔒123");

            when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(authService, times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("TC-SV-008: Password có SQL injection pattern")
        void testLogin_PasswordWithSQLInjection_ShouldBeHandledSafely() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("test@example.com");
            request.setPassword("' OR '1'='1");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new UnauthorizedException("Tài khoản hoặc mật khẩu không đúng"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());

            verify(authService, times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("TC-SV-009: Password có XSS pattern")
        void testLogin_PasswordWithXSS_ShouldBeHandledSafely() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("test@example.com");
            request.setPassword("<script>alert('XSS')</script>");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new UnauthorizedException("Tài khoản hoặc mật khẩu không đúng"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());

            verify(authService, times(1)).login(any(LoginRequest.class));
        }
    }

    @Nested
    @DisplayName("5. Kiểm thử trường hợp nhập sai email")
    class ErrorTests_InvalidEmail {

        @Test
        @DisplayName("TC-ERR-001: Email không tồn tại trong hệ thống - Trả về 401")
        void testLogin_NonExistentEmail_ShouldReturn401() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("nonexistent@example.com");
            request.setPassword("ValidPass123");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new UnauthorizedException("Tài khoản hoặc mật khẩu không đúng"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Tài khoản hoặc mật khẩu không đúng"));

            verify(authService, times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("TC-ERR-002: Email sai format hoàn toàn - Backend vẫn xử lý")
        void testLogin_CompletelyInvalidEmail_ShouldStillProcess() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("not-an-email");
            request.setPassword("ValidPass123");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new UnauthorizedException("Tài khoản hoặc mật khẩu không đúng"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());

            verify(authService, times(1)).login(any(LoginRequest.class));
        }
    }

    @Nested
    @DisplayName("6. Kiểm thử trường hợp để trống password")
    class ErrorTests_EmptyPassword {

        @Test
        @DisplayName("TC-ERR-003: Để trống password với email hợp lệ - Trả về 400")
        void testLogin_EmptyPasswordWithValidEmail_ShouldReturn400() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("test@example.com");
            request.setPassword("");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Vui lòng nhập mật khẩu"));

            verify(authService, never()).login(any());
        }

        @Test
        @DisplayName("TC-ERR-004: Để trống cả email và password - Trả về 400 với nhiều lỗi")
        void testLogin_BothEmpty_ShouldReturn400WithMultipleErrors() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("");
            request.setPassword("");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any());
        }
    }

    @Nested
    @DisplayName("7. Kiểm thử trường hợp login thành công")
    class SuccessTests {

        @Test
        @DisplayName("TC-SUC-001: Login thành công với email và password hợp lệ")
        void testLogin_ValidCredentials_ShouldReturnSuccess() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("test@example.com");
            request.setPassword("ValidPass123");

            when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Đăng nhập thành công"))
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.refreshToken").exists())
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.expiresIn").value(3600))
                    .andExpect(jsonPath("$.data.user.id").exists())
                    .andExpect(jsonPath("$.data.user.email").value("test@example.com"))
                    .andExpect(jsonPath("$.data.user.fullName").value("Test User"))
                    .andExpect(jsonPath("$.data.user.role").value("USER"));

            verify(authService, times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("TC-SUC-002: Login thành công với số điện thoại")
        void testLogin_ValidPhone_ShouldReturnSuccess() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("0901234567");
            request.setPassword("SecurePassword789");

            AuthResponse phoneAuthResponse = AuthResponse.builder()
                    .accessToken("token")
                    .refreshToken("refresh")
                    .tokenType("Bearer")
                    .expiresIn(3600L)
                    .user(AuthResponse.UserInfo.builder()
                            .id("550e8400-e29b-41d4-a716-446655440000")
                            .phone("0901234567")
                            .fullName("Test User")
                            .role("USER")
                            .build())
                    .build();

            when(authService.login(any(LoginRequest.class))).thenReturn(phoneAuthResponse);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.user.phone").value("0901234567"));

            verify(authService, times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("TC-SUC-003: Response chứa đầy đủ thông tin cần thiết")
        void testLogin_SuccessResponse_ContainsAllRequiredFields() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("test@example.com");
            request.setPassword("ValidPass123");

            when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").isBoolean())
                    .andExpect(jsonPath("$.message").isString())
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.data.accessToken").isString())
                    .andExpect(jsonPath("$.data.refreshToken").isString())
                    .andExpect(jsonPath("$.data.tokenType").isString())
                    .andExpect(jsonPath("$.data.expiresIn").isNumber())
                    .andExpect(jsonPath("$.data.user").exists())
                    .andExpect(jsonPath("$.data.user.id").isString())
                    .andExpect(jsonPath("$.data.user.email").isString())
                    .andExpect(jsonPath("$.data.user.fullName").isString())
                    .andExpect(jsonPath("$.data.user.role").isString());

            verify(authService, times(1)).login(any(LoginRequest.class));
        }
    }

    @Nested
    @DisplayName("8. Kiểm thử Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("TC-ERR-005: Xử lý lỗi 401 Unauthorized")
        void testLogin_Unauthorized_ShouldReturn401() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("test@example.com");
            request.setPassword("WrongPassword");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new UnauthorizedException("Tài khoản hoặc mật khẩu không đúng"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Tài khoản hoặc mật khẩu không đúng"));
        }

        @Test
        @DisplayName("TC-ERR-006: Xử lý tài khoản bị vô hiệu hóa")
        void testLogin_DisabledAccount_ShouldReturn401() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("disabled@example.com");
            request.setPassword("ValidPass123");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new UnauthorizedException("Tài khoản đã bị vô hiệu hóa"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Tài khoản đã bị vô hiệu hóa"));
        }

        @Test
        @DisplayName("TC-ERR-007: Xử lý invalid JSON format")
        void testLogin_InvalidJSON_ShouldReturn400() throws Exception {
            String invalidJson = "{ invalid json }";

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any());
        }
    }

    @Nested
    @DisplayName("9. Kiểm thử Security")
    class SecurityTests {

        @Test
        @DisplayName("TC-SEC-001: Response không chứa password hash")
        void testLogin_ResponseShouldNotContainPasswordHash() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("test@example.com");
            request.setPassword("ValidPass123!");

            when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist())
                    .andExpect(jsonPath("$.data.user.password").doesNotExist());

            verify(authService, times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("TC-SEC-002: JWT token format validation")
        void testLogin_JWTTokenFormatValidation() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("test@example.com");
            request.setPassword("ValidPass123!");

            when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").isString())
                    .andExpect(jsonPath("$.data.accessToken").value(org.hamcrest.Matchers.matchesRegex("^[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+$")));
        }

        @Test
        @DisplayName("TC-SEC-003: HTTPS only in production")
        void testLogin_HTTPSEnforcement() throws Exception {
            // This test verifies security headers are set
            LoginRequest request = new LoginRequest();
            request.setIdentifier("test@example.com");
            request.setPassword("ValidPass123!");

            when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("X-Content-Type-Options"))
                    .andExpect(header().exists("X-Frame-Options"));
        }
    }

    @Nested
    @DisplayName("10. Kiểm thử Rate Limiting")
    class RateLimitingTests {

        @Test
        @DisplayName("TC-RATE-001: Multiple failed login attempts")
        void testLogin_MultipleFailedAttempts() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setIdentifier("test@example.com");
            request.setPassword("WrongPassword");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new UnauthorizedException("Tài khoản hoặc mật khẩu không đúng"));

            // Simulate 5 failed attempts
            for (int i = 0; i < 5; i++) {
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isUnauthorized());
            }

            // Note: Actual rate limiting would be tested in integration tests
            verify(authService, times(5)).login(any(LoginRequest.class));
        }
    }
}