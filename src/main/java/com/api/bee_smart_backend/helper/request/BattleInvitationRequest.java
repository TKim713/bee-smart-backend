package com.api.bee_smart_backend.helper.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class BattleInvitationRequest {
    @NotBlank(message = "Invitee Id is required")
    private String inviteeId;

    @NotBlank(message = "Grade ID is required")
    private String gradeId;

    @NotBlank(message = "Subject ID is required")
    private String subjectId;

    private String topic; // Optional topic/description
}

