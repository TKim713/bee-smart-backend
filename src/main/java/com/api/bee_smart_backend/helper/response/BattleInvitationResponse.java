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
public class BattleInvitationResponse {
    private String invitationId;
    private UserResponse inviter;
    private UserResponse invitee;
    private String gradeId;
    private String subjectId;
    private String topic;
    private String status;
    private String battleId;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant expiresAt;
}

