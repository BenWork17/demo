package com.baohoanhao.demo.service;

import com.baohoanhao.demo.dto.request.DeleteAccountRequest;
import com.baohoanhao.demo.dto.request.UpdateProfileRequest;
import com.baohoanhao.demo.dto.response.UserProfileResponse;
import com.baohoanhao.demo.entity.Role;
import com.baohoanhao.demo.entity.User;
import com.baohoanhao.demo.exception.BadRequestException;
import com.baohoanhao.demo.exception.ForbiddenException;
import com.baohoanhao.demo.exception.ResourceNotFoundException;
import com.baohoanhao.demo.repository.UserRepository;
import com.baohoanhao.demo.security.TokenStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenStorageService tokenStorageService;

    @InjectMocks
    private UserService userService;

    private TestFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new TestFixtures();
    }

    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfileTests {

        @Test
        @DisplayName("should update full name successfully when valid request")
        void updateProfile_ValidFullName_Success() {
            // Arrange
            User user = fixtures.createActiveUser();
            UpdateProfileRequest request = fixtures.createUpdateRequest("New Name", user.getEmail(), user.getPhone());

            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            // Act
            UserProfileResponse response = userService.updateProfile(user.getId().toString(), request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getFullName()).isEqualTo("New Name");
            verify(userRepository).save(argThat(u -> u.getFullName().equals("New Name")));
        }

        @Test
        @DisplayName("should update email successfully when new email is unique")
        void updateProfile_UniqueEmail_Success() {
            // Arrange
            User user = fixtures.createActiveUser();
            UpdateProfileRequest request = fixtures.createUpdateRequest(
                user.getFullName(), 
                "newemail@example.com", 
                user.getPhone()
            );

            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(user);

            // Act
            UserProfileResponse response = userService.updateProfile(user.getId().toString(), request);

            // Assert
            assertThat(response).isNotNull();
            verify(userRepository).existsByEmail("newemail@example.com");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should throw BadRequestException when email already exists")
        void updateProfile_DuplicateEmail_ThrowsBadRequestException() {
            // Arrange
            User user = fixtures.createActiveUser();
            UpdateProfileRequest request = fixtures.createUpdateRequest(
                user.getFullName(),
                "duplicate@example.com",
                user.getPhone()
            );

            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> userService.updateProfile(user.getId().toString(), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email đã được sử dụng");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("should not check email uniqueness when email unchanged")
        void updateProfile_SameEmail_SkipsUniqueCheck() {
            // Arrange
            User user = fixtures.createActiveUser();
            UpdateProfileRequest request = fixtures.createUpdateRequest(
                "New Name",
                user.getEmail(),
                user.getPhone()
            );

            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            // Act
            userService.updateProfile(user.getId().toString(), request);

            // Assert
            verify(userRepository, never()).existsByEmail(anyString());
        }

        @Test
        @DisplayName("should update phone successfully when new phone is unique")
        void updateProfile_UniquePhone_Success() {
            // Arrange
            User user = fixtures.createActiveUser();
            UpdateProfileRequest request = fixtures.createUpdateRequest(
                user.getFullName(),
                user.getEmail(),
                "0987654321"
            );

            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(userRepository.existsByPhone("0987654321")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(user);

            // Act
            UserProfileResponse response = userService.updateProfile(user.getId().toString(), request);

            // Assert
            assertThat(response).isNotNull();
            verify(userRepository).existsByPhone("0987654321");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should throw BadRequestException when phone already exists")
        void updateProfile_DuplicatePhone_ThrowsBadRequestException() {
            // Arrange
            User user = fixtures.createActiveUser();
            UpdateProfileRequest request = fixtures.createUpdateRequest(
                user.getFullName(),
                user.getEmail(),
                "0999999999"
            );

            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(userRepository.existsByPhone("0999999999")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> userService.updateProfile(user.getId().toString(), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Số điện thoại đã được sử dụng");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("should not check phone uniqueness when phone unchanged")
        void updateProfile_SamePhone_SkipsUniqueCheck() {
            // Arrange
            User user = fixtures.createActiveUser();
            UpdateProfileRequest request = fixtures.createUpdateRequest(
                "New Name",
                user.getEmail(),
                user.getPhone()
            );

            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            // Act
            userService.updateProfile(user.getId().toString(), request);

            // Assert
            verify(userRepository, never()).existsByPhone(anyString());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void updateProfile_UserNotFound_ThrowsResourceNotFoundException() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UpdateProfileRequest request = fixtures.createUpdateRequest("Name", "email@test.com", "0912345678");

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.updateProfile(userId.toString(), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy thông tin người dùng");
        }

        @Test
        @DisplayName("should throw BadRequestException when userId is invalid UUID")
        void updateProfile_InvalidUserId_ThrowsBadRequestException() {
            // Arrange
            UpdateProfileRequest request = fixtures.createUpdateRequest("Name", "email@test.com", "0912345678");

            // Act & Assert
            assertThatThrownBy(() -> userService.updateProfile("invalid-uuid", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ID người dùng không hợp lệ");
        }
    }

    @Nested
    @DisplayName("deleteAccount()")
    class DeleteAccountTests {

        @Test
        @DisplayName("should deactivate user account when valid password")
        void deleteAccount_ValidPassword_DeactivatesAccount() {
            // Arrange
            User user = fixtures.createActiveUser();
            DeleteAccountRequest request = new DeleteAccountRequest();
            request.setPassword("correctPassword");

            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("correctPassword", user.getPasswordHash())).thenReturn(true);
            when(userRepository.save(any(User.class))).thenReturn(user);

            // Act
            userService.deleteAccount(user.getId().toString(), request);

            // Assert
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().isActive()).isFalse();
        }

        @Test
        @DisplayName("should revoke all tokens when account deleted")
        void deleteAccount_Success_RevokesAllTokens() {
            // Arrange
            User user = fixtures.createActiveUser();
            DeleteAccountRequest request = new DeleteAccountRequest();
            request.setPassword("correctPassword");

            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("correctPassword", user.getPasswordHash())).thenReturn(true);
            when(userRepository.save(any(User.class))).thenReturn(user);

            // Act
            userService.deleteAccount(user.getId().toString(), request);

            // Assert
            verify(tokenStorageService).revokeAllUserTokens(user.getId().toString());
        }

        @Test
        @DisplayName("should throw BadRequestException when password incorrect")
        void deleteAccount_IncorrectPassword_ThrowsBadRequestException() {
            // Arrange
            User user = fixtures.createActiveUser();
            DeleteAccountRequest request = new DeleteAccountRequest();
            request.setPassword("wrongPassword");

            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongPassword", user.getPasswordHash())).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> userService.deleteAccount(user.getId().toString(), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Mật khẩu không chính xác");

            verify(userRepository, never()).save(any(User.class));
            verify(tokenStorageService, never()).revokeAllUserTokens(anyString());
        }

        @Test
        @DisplayName("should throw ForbiddenException when deleting admin account")
        void deleteAccount_AdminUser_ThrowsForbiddenException() {
            // Arrange
            User adminUser = fixtures.createAdminUser();
            DeleteAccountRequest request = new DeleteAccountRequest();
            request.setPassword("correctPassword");

            when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
            when(passwordEncoder.matches("correctPassword", adminUser.getPasswordHash())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> userService.deleteAccount(adminUser.getId().toString(), request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Không thể xóa tài khoản quản trị viên");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void deleteAccount_UserNotFound_ThrowsResourceNotFoundException() {
            // Arrange
            UUID userId = UUID.randomUUID();
            DeleteAccountRequest request = new DeleteAccountRequest();
            request.setPassword("password");

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.deleteAccount(userId.toString(), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy thông tin người dùng");
        }

        @Test
        @DisplayName("should throw BadRequestException when userId is invalid UUID")
        void deleteAccount_InvalidUserId_ThrowsBadRequestException() {
            // Arrange
            DeleteAccountRequest request = new DeleteAccountRequest();
            request.setPassword("password");

            // Act & Assert
            assertThatThrownBy(() -> userService.deleteAccount("invalid-uuid", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ID người dùng không hợp lệ");
        }
    }

    static class TestFixtures {
        
        User createActiveUser() {
            return User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .phone("0912345678")
                .fullName("Test User")
                .passwordHash("$2a$10$hashedPassword")
                .role(Role.USER)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        }

        User createAdminUser() {
            return User.builder()
                .id(UUID.randomUUID())
                .email("admin@example.com")
                .phone("0987654321")
                .fullName("Admin User")
                .passwordHash("$2a$10$hashedPassword")
                .role(Role.ADMIN)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        }

        UpdateProfileRequest createUpdateRequest(String fullName, String email, String phone) {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setFullName(fullName);
            request.setEmail(email);
            request.setPhone(phone);
            return request;
        }
    }
}
