// RegisterRequest.java
package com.baohoanhao.demo.dto.request;

import com.baohoanhao.demo.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    // Có thể null nếu user đăng ký bằng SĐT, nhưng ta sẽ validate logic ở Service
    @Email(message = "Email không hợp lệ")
    private String email;

    // Validate định dạng số điện thoại (VN)
    // @Pattern(regexp = "(84|0[3|5|7|8|9])+([0-9]{8})", message = "Số điện thoại không hợp lệ")
    private String phone;

    @StrongPassword // Sử dụng Annotation xịn sò vừa viết
    private String password;
}