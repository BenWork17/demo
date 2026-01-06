package com.baohoanhao.demo.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Vui lòng nhập Email hoặc Số điện thoại")
    @JsonAlias({"email", "phone"})
    private String identifier;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    private String password;
}