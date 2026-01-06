package com.baohoanhao.demo.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) return false;
        // Regex kiểm tra độ mạnh mật khẩu
        // (?=.*[0-9]): Ít nhất 1 số
        // (?=.*[a-z]): Ít nhất 1 chữ thường
        // (?=.*[A-Z]): Ít nhất 1 chữ hoa
        // (?=.*[!@#$%^&*]): Ít nhất 1 ký tự đặc biệt
        // .{8,}: Độ dài tối thiểu 8
        return password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*]).{8,}$");
    }
}