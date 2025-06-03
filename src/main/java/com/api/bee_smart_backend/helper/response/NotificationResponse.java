package com.api.bee_smart_backend.helper.response;

import com.api.bee_smart_backend.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class NotificationResponse {
    private String notificationId;
    private String title;
    private String message;
    private String link;
    private String type; // QUIZ, BATTLE, SYSTEM, etc.
    private boolean read;
    private Instant createdAt;
}
