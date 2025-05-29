package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.config.NotificationWebSocketHandler;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.BattleInvitationRequest;
import com.api.bee_smart_backend.helper.request.BattleRequest;
import com.api.bee_smart_backend.helper.response.BattleInvitationResponse;
import com.api.bee_smart_backend.helper.response.BattleResponse;
import com.api.bee_smart_backend.helper.response.InvitationActionResponse;
import com.api.bee_smart_backend.model.BattleInvitation;
import com.api.bee_smart_backend.model.Notification;
import com.api.bee_smart_backend.model.User;
import com.api.bee_smart_backend.repository.BattleInvitationRepository;
import com.api.bee_smart_backend.repository.NotificationRepository;
import com.api.bee_smart_backend.repository.TokenRepository;
import com.api.bee_smart_backend.repository.UserRepository;
import com.api.bee_smart_backend.service.BattleInvitationService;
import com.api.bee_smart_backend.service.BattleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BattleInvitationServiceImpl implements BattleInvitationService {

    @Autowired
    private final BattleInvitationRepository invitationRepository;
    @Autowired
    private final TokenRepository tokenRepository;
    @Autowired
    private final UserRepository userRepository;
    @Autowired
    private final NotificationRepository notificationRepository;
    @Autowired
    private final BattleService battleService;
    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final MapData mapData;

    private String getUserIdFromToken(String token) {
        return tokenRepository.findByAccessToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"))
                .getUser().getUserId();
    }

    @Override
    public BattleInvitationResponse getInvitationById(String token, String invitationId) {
        String userId = getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        BattleInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new CustomException("Không tìm thấy lời mời", HttpStatus.NOT_FOUND));

        // Check if user is either inviter or invitee
        if (!invitation.getInviter().getUserId().equals(userId) &&
                !invitation.getInvitee().getUserId().equals(userId)) {
            throw new CustomException("Bạn không có quyền xem lời mời này", HttpStatus.FORBIDDEN);
        }

        return mapData.mapOne(invitation, BattleInvitationResponse.class);
    }

    @Override
    public BattleInvitationResponse sendInvitation(String token, BattleInvitationRequest request) {
        String inviterId = getUserIdFromToken(token);

        User inviter = userRepository.findById(inviterId)
                .orElseThrow(() -> new CustomException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        User invitee = userRepository.findByUserIdAndDeletedAtIsNull(request.getInviteeId())
                .orElseThrow(() -> new CustomException("Không tìm thấy người dùng được mời", HttpStatus.NOT_FOUND));

        // Can't invite yourself
        if (inviter.getUserId().equals(invitee.getUserId())) {
            throw new CustomException("Không thể mời chính mình", HttpStatus.BAD_REQUEST);
        }

        // Check if there's already a pending invitation
        if (invitationRepository.existsByInviterAndInviteeAndStatus(inviter, invitee, "PENDING")) {
            throw new CustomException("Đã có lời mời đang chờ xử lý", HttpStatus.BAD_REQUEST);
        }

//        Optional<BattleResponse> activeBattle = battleService.getActiveBattleForUser(inviter.getUserId());
//        if (activeBattle.isPresent()) {
//            throw new CustomException("Bạn đang trong một trận đấu khác", HttpStatus.BAD_REQUEST);
//        }
//
//        Optional<BattleResponse> activeBattleInvitee = battleService.getActiveBattleForUser(invitee.getUserId());
//        if (activeBattleInvitee.isPresent()) {
//            throw new CustomException("Người được mời đang trong trận đấu khác", HttpStatus.BAD_REQUEST);
//        }

        List<BattleInvitation> recentInvitations = invitationRepository
                .findByInviterAndCreatedAtAfter(inviter, Instant.now().minus(1, ChronoUnit.MINUTES));
        if (recentInvitations.size() >= 3) {
            throw new CustomException("Bạn đã gửi quá nhiều lời mời. Vui lòng đợi.", HttpStatus.TOO_MANY_REQUESTS);
        }

        // Create invitation
        BattleInvitation invitation = BattleInvitation.builder()
                .inviter(inviter)
                .invitee(invitee)
                .gradeId(request.getGradeId())
                .subjectId(request.getSubjectId())
                .topic(request.getTopic())
                .status("PENDING")
                .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES)) // 5 minutes expiry
                .createdAt(Instant.now())
                .build();

        invitation = invitationRepository.save(invitation);

        // Create notification
        Notification notification = Notification.builder()
                .user(invitee)
                .title("Lời mời thách đấu")
                .message(inviter.getUsername() + " đã mời bạn tham gia trận đấu")
                .link("/battle/invitation/" + invitation.getInvitationId())
                .type("BATTLE_INVITATION")
                .read(false)
                .build();

        notification = notificationRepository.save(notification);
        //notificationWebSocketHandler.sendInvitationUpdate(invitee.getUserId(), "BATTLE_INVITATION_RECEIVED", invitation);

        // Send notification via WebSocket
        notificationWebSocketHandler.sendNotification(invitee.getUserId(), notification);

        log.info("Battle invitation sent from {} to {}", inviter.getUsername(), invitee.getUsername());

        return mapData.mapOne(invitation, BattleInvitationResponse.class);
    }

    @Override
    public InvitationActionResponse acceptInvitation(String token, String invitationId) {
        String userId = getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        BattleInvitation invitation = invitationRepository.findByInvitationIdAndInvitee(invitationId, user)
                .orElseThrow(() -> new CustomException("Không tìm thấy lời mời", HttpStatus.NOT_FOUND));

        if (!"PENDING".equals(invitation.getStatus())) {
            throw new CustomException("Lời mời không hợp lệ hoặc đã được xử lý", HttpStatus.BAD_REQUEST);
        }

        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            invitation.setStatus("EXPIRED");
            invitationRepository.save(invitation);
            throw new CustomException("Lời mời đã hết hạn", HttpStatus.BAD_REQUEST);
        }

        // Create battle
        BattleRequest battleRequest = BattleRequest.builder()
                .topic(invitation.getTopic())
                .gradeId(invitation.getGradeId())
                .subjectId(invitation.getSubjectId())
                .playerIds(List.of(invitation.getInviter().getUserId(), invitation.getInvitee().getUserId()))
                .build();

        BattleResponse battle = battleService.createBattle(battleRequest);

        // Update invitation
        invitation.setStatus("ACCEPTED");
        invitation.setBattleId(battle.getBattleId());
        invitation.setUpdatedAt(Instant.now());
        invitationRepository.save(invitation);

        // Send immediate battle redirect message to inviter (no notification needed)
        notificationWebSocketHandler.sendBattleAcceptedMessage(
                invitation.getInviter().getUserId(),
                battle.getBattleId()
        );

        // Create notification for inviter (for history purposes, but won't trigger browser notification)
        Notification inviterNotification = Notification.builder()
                .user(invitation.getInviter())
                .title("Lời mời được chấp nhận")
                .message(user.getUsername() + " đã chấp nhận lời mời thách đấu")
                .link("/battle-detail/" + battle.getBattleId()) // Update to use battle-detail
                .type("BATTLE_ACCEPTED")
                .read(true) // Mark as read since user is auto-redirected
                .build();

        notificationRepository.save(inviterNotification);

        log.info("Battle invitation {} accepted, battle {} created", invitationId, battle.getBattleId());

        return InvitationActionResponse.builder()
                .message("Đã chấp nhận lời mời thành công")
                .status("ACCEPTED")
                .battleId(battle.getBattleId())
                .invitation(mapData.mapOne(invitation, BattleInvitationResponse.class))
                .build();
    }

    @Override
    public InvitationActionResponse declineInvitation(String token, String invitationId) {
        String userId = getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        BattleInvitation invitation = invitationRepository.findByInvitationIdAndInvitee(invitationId, user)
                .orElseThrow(() -> new CustomException("Không tìm thấy lời mời", HttpStatus.NOT_FOUND));

        if (!"PENDING".equals(invitation.getStatus())) {
            throw new CustomException("Lời mời không hợp lệ hoặc đã được xử lý", HttpStatus.BAD_REQUEST);
        }

        invitation.setStatus("DECLINED");
        invitation.setUpdatedAt(Instant.now());
        invitationRepository.save(invitation);

        // Notify inviter that invitation was declined
        Notification inviterNotification = Notification.builder()
                .user(invitation.getInviter())
                .title("Lời mời bị từ chối")
                .message(user.getUsername() + " đã từ chối lời mời thách đấu")
                .type("BATTLE_DECLINED")
                .read(false)
                .build();

        notificationRepository.save(inviterNotification);
        //notificationWebSocketHandler.sendInvitationUpdate(invitation.getInviter().getUserId(), "BATTLE_INVITATION_DECLINED", invitation);
        notificationWebSocketHandler.sendNotification(invitation.getInviter().getUserId(), inviterNotification);

        log.info("Battle invitation {} declined", invitationId);

        return InvitationActionResponse.builder()
                .message("Đã từ chối lời mời")
                .status("DECLINED")
                .invitation(mapData.mapOne(invitation, BattleInvitationResponse.class))
                .build();
    }

    @Override
    public List<BattleInvitationResponse> getPendingInvitations(String token) {
        String userId = getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        List<BattleInvitation> invitations = invitationRepository.findByInviteeAndStatusOrderByCreatedAtDesc(user, "PENDING");

        // Filter out expired invitations
        invitations = invitations.stream()
                .filter(inv -> inv.getExpiresAt().isAfter(Instant.now()))
                .toList();

        return mapData.mapList(invitations, BattleInvitationResponse.class);
    }

    @Override
    public List<BattleInvitationResponse> getSentInvitations(String token) {
        String userId = getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        List<BattleInvitation> invitations = invitationRepository.findByInviterOrderByCreatedAtDesc(user);
        return mapData.mapList(invitations, BattleInvitationResponse.class);
    }

    @Override
    public InvitationActionResponse cancelInvitation(String token, String invitationId) {
        String userId = getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        BattleInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new CustomException("Không tìm thấy lời mời", HttpStatus.NOT_FOUND));

        if (!invitation.getInviter().getUserId().equals(userId)) {
            throw new CustomException("Bạn không có quyền hủy lời mời này", HttpStatus.FORBIDDEN);
        }

        if (!"PENDING".equals(invitation.getStatus())) {
            throw new CustomException("Không thể hủy lời mời đã được xử lý", HttpStatus.BAD_REQUEST);
        }

        invitation.setStatus("CANCELLED");
        invitation.setUpdatedAt(Instant.now());
        invitationRepository.save(invitation);

        log.info("Battle invitation {} cancelled by inviter", invitationId);

        return InvitationActionResponse.builder()
                .message("Đã hủy lời mời thành công")
                .status("CANCELLED")
                .invitation(mapData.mapOne(invitation, BattleInvitationResponse.class))
                .build();
    }

    @Scheduled(fixedRate = 300000)
    @Override
    public void cleanupExpiredInvitations() {
        List<BattleInvitation> expiredInvitations = invitationRepository
                .findByStatusAndExpiresAtBefore("PENDING", Instant.now());

        for (BattleInvitation invitation : expiredInvitations) {
            invitation.setStatus("EXPIRED");
            invitation.setUpdatedAt(Instant.now());
            invitationRepository.save(invitation);
        }

        if (!expiredInvitations.isEmpty()) {
            log.info("Cleaned up {} expired battle invitations", expiredInvitations.size());
        }
    }
}