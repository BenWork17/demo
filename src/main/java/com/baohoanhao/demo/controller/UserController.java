package com.baohoanhao.demo.controller;

import com.baohoanhao.demo.dto.request.DeleteAccountRequest;
import com.baohoanhao.demo.dto.request.UpdateProfileRequest;
import com.baohoanhao.demo.dto.response.ApiResponse;
import com.baohoanhao.demo.dto.response.UserProfileResponse;
import com.baohoanhao.demo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Quản lý thông tin người dùng")
public class UserController {

    private final UserService userService;

    @PutMapping("/profile")
    @Operation(summary = "Cập nhật thông tin cá nhân")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {

        String userId = authentication.getName();
        UserProfileResponse response = userService.updateProfile(userId, request);

        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin thành công", response));
    }

    @PostMapping("/profile/delete")
    @Operation(summary = "Xóa tài khoản")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @Valid @RequestBody DeleteAccountRequest request,
            Authentication authentication) {

        String userId = authentication.getName();
        userService.deleteAccount(userId, request);

        return ResponseEntity.ok(ApiResponse.success("Tài khoản đã được xóa thành công", null));
    }
}
