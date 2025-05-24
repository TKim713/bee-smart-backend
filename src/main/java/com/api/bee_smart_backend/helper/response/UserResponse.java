package com.api.bee_smart_backend.helper.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UserResponse {
    private String userId;
    private String username;
    private String email;
    private String role;
    private boolean enabled;
    private boolean active;
    private boolean isOnline;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
