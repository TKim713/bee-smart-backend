package com.api.bee_smart_backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "battle_invitation")
public class BattleInvitation {
    @Id
    private String invitationId;

    @DBRef(lazy = true)
    private User inviter; // User who sent the invitation

    @DBRef(lazy = true)
    private User invitee; // User who received the invitation

    private String gradeId;
    private String subjectId;
    private String topic;

    @Builder.Default
    private String status = "PENDING"; // PENDING, ACCEPTED, DECLINED, EXPIRED

    private String battleId; // Set when invitation is accepted and battle is created

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
    @LastModifiedDate
    private Instant expiresAt;
}
