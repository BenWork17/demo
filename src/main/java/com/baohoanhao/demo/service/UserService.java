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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenStorageService tokenStorageService;

    @Transactional
    public UserProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
        UUID userUuid = parseUserId(userId);
        User user = userRepository.findById(userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin người dùng"));

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email đã được sử dụng bởi tài khoản khác");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw new BadRequestException("Số điện thoại đã được sử dụng bởi tài khoản khác");
            }
            user.setPhone(request.getPhone());
        }

        user.setFullName(request.getFullName());
        userRepository.save(user);

        return buildUserProfileResponse(user);
    }

    @Transactional
    public void deleteAccount(String userId, DeleteAccountRequest request) {
        UUID userUuid = parseUserId(userId);
        User user = userRepository.findById(userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin người dùng"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu không chính xác");
        }

        if (user.getRole() == Role.ADMIN) {
            throw new ForbiddenException("Không thể xóa tài khoản quản trị viên");
        }

        user.setActive(false);
        userRepository.save(user);

        tokenStorageService.revokeAllUserTokens(userId);
    }

    private UUID parseUserId(String userId) {
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("ID người dùng không hợp lệ");
        }
    }

    private UserProfileResponse buildUserProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
