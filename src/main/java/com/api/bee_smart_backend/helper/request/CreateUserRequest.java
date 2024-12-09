package com.api.bee_smart_backend.helper.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CreateUserRequest {
    private String fullName;
    private String username;
    @Email(message = "Email không hợp lệ")
    @Pattern(regexp = "^[\\w.%+-]+@[\\w.-]+\\.(com|vn)$", message = "Email không hợp lệ")
    private String email;
    @Size(min = 8, max = 50, message = "Mật khẩu phải có từ 8 đến 50 ký tự")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[@$!%*#?&]).*$", message = "Mật khẩu phải chứa ít nhất một chữ thường, một chữ hoa và một ký tự đặc biệt (@$!%*#?&)")
    private String password;
    private String role;

    private String district;
    private String city;
    private String grade;
    private String school;
}
