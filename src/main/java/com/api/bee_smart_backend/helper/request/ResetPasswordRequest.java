package com.api.bee_smart_backend.helper.request;

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
public class ResetPasswordRequest {
    private String token;
    @Size(min = 8, max = 50, message = "Mật khẩu phải có từ 8 đến 50 ký tự")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[@$!%*#?&]).*$", message = "Mật khẩu phải chứa ít nhất một chữ thường, một chữ hoa và một ký tự đặc biệt (@$!%*#?&)")
    private String newPassword;
}
