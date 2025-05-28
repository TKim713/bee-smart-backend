package com.api.bee_smart_backend.helper.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationActionResponse {
    private String message;
    private String status;
    private String battleId; // Only present if invitation was accepted
    private BattleInvitationResponse invitation;
}
