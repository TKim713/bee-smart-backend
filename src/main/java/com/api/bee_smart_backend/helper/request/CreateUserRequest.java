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
    private String username;
    @Email(message = "Email should be valid")
    private String email;
    @Size(min = 6, max = 18, message = "Password should be between 6 and 18 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[@$!%*#?&]).*$", message = "Password should contain at least one lowercase, one uppercase, and one special character (@$!%*#?&)")
    private String password;
    private String role;
}
