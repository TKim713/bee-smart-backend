package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.BattleInvitationRequest;
import com.api.bee_smart_backend.helper.response.BattleInvitationResponse;
import com.api.bee_smart_backend.helper.response.InvitationActionResponse;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

public interface BattleInvitationService {
    BattleInvitationResponse getInvitationById(String token, String invitationId);

    BattleInvitationResponse sendInvitation(String token, BattleInvitationRequest request);

    InvitationActionResponse acceptInvitation(String token, String invitationId);

    InvitationActionResponse declineInvitation(String token, String invitationId);

    List<BattleInvitationResponse> getPendingInvitations(String token);

    List<BattleInvitationResponse> getSentInvitations(String token);

    InvitationActionResponse cancelInvitation(String token, String invitationId);

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    void cleanupExpiredInvitations();
}
