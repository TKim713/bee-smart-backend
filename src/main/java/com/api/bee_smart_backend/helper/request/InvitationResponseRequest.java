package com.api.bee_smart_backend.helper.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO for responding to invitations
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationResponseRequest {

    @NotBlank(message = "Invitation ID is required")
    private String invitationId;

    @NotBlank(message = "Response is required")
    private String response; // ACCEPT or DECLINE
}
