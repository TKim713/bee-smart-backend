package com.api.bee_smart_backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
import com.api.bee_smart_backend.model.Token;
import com.api.bee_smart_backend.model.User;
import com.api.bee_smart_backend.repository.BattleInvitationRepository;
import com.api.bee_smart_backend.repository.NotificationRepository;
import com.api.bee_smart_backend.repository.TokenRepository;
import com.api.bee_smart_backend.repository.UserRepository;
import com.api.bee_smart_backend.service.impl.BattleInvitationServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

@Slf4j
public class BattleInvitationServiceImplTest {

    @Mock
    private BattleInvitationRepository invitationRepository;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private BattleService battleService;

    @Mock
    private NotificationWebSocketHandler notificationWebSocketHandler;

    @Mock
    private MapData mapData;

    @InjectMocks
    private BattleInvitationServiceImpl battleInvitationService;

    private User inviter;
    private User invitee;
    private Token token;
    private BattleInvitation invitation;
    private BattleInvitationRequest invitationRequest;
    private BattleResponse battleResponse;
    private Notification notification;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        inviter = User.builder()
                .userId("1")
                .username("inviter")
                .email("inviter@example.com")
                .build();

        invitee = User.builder()
                .userId("2")
                .username("invitee")
                .email("invitee@example.com")
                .build();

        token = Token.builder()
                .accessToken("validToken")
                .user(inviter)
                .build();

        invitation = BattleInvitation.builder()
                .invitationId("inv1")
                .inviter(inviter)
                .invitee(invitee)
                .gradeId("10")
                .subjectId("math")
                .topic("Algebra")
                .status("PENDING")
                .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .createdAt(Instant.now())
                .build();

        invitationRequest = BattleInvitationRequest.builder()
                .inviteeId("2")
                .gradeId("10")
                .subjectId("math")
                .topic("Algebra")
                .build();

        battleResponse = BattleResponse.builder()
                .battleId("battle1")
                .build();

        notification = Notification.builder()
                .user(invitee)
                .title("Lời mời thách đấu")
                .message("inviter đã mời bạn tham gia trận đấu")
                .link("/battle/invitation/inv1")
                .type("BATTLE_INVITATION")
                .read(false)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    public void testGetInvitationById_Success_Inviter() {
        when(tokenRepository.findByAccessToken("validToken")).thenReturn(Optional.of(token));
        when(userRepository.findById("1")).thenReturn(Optional.of(inviter));
        when(invitationRepository.findById("inv1")).thenReturn(Optional.of(invitation));
        when(mapData.mapOne(invitation, BattleInvitationResponse.class)).thenReturn(new BattleInvitationResponse());

        BattleInvitationResponse response = battleInvitationService.getInvitationById("validToken", "inv1");

        assertNotNull(response);
        verify(tokenRepository).findByAccessToken("validToken");
        verify(userRepository).findById("1");
        verify(invitationRepository).findById("inv1");
        verify(mapData).mapOne(invitation, BattleInvitationResponse.class);
    }

    @Test
    public void testGetInvitationById_UnauthorizedUser() {
        User unauthorizedUser = User.builder().userId("3").build();
        Token unauthorizedToken = Token.builder().accessToken("otherToken").user(unauthorizedUser).build();

        when(tokenRepository.findByAccessToken("otherToken")).thenReturn(Optional.of(unauthorizedToken));
        when(userRepository.findById("3")).thenReturn(Optional.of(unauthorizedUser));
        when(invitationRepository.findById("inv1")).thenReturn(Optional.of(invitation));

        CustomException exception = assertThrows(CustomException.class, () -> {
            battleInvitationService.getInvitationById("otherToken", "inv1");
        });

        assertEquals("Bạn không có quyền xem lời mời này", exception.getMessage());
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    public void testSendInvitation_Success() {
        when(tokenRepository.findByAccessToken("validToken")).thenReturn(Optional.of(token));
        when(userRepository.findById("1")).thenReturn(Optional.of(inviter));
        when(userRepository.findByUserIdAndDeletedAtIsNull("2")).thenReturn(Optional.of(invitee));
        when(invitationRepository.existsByInviterAndInviteeAndStatus(inviter, invitee, "PENDING")).thenReturn(false);
        when(invitationRepository.findByInviterAndCreatedAtAfter(eq(inviter), any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(invitationRepository.save(any(BattleInvitation.class))).thenReturn(invitation);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(mapData.mapOne(invitation, BattleInvitationResponse.class)).thenReturn(new BattleInvitationResponse());

        BattleInvitationResponse response = battleInvitationService.sendInvitation("validToken", invitationRequest);

        assertNotNull(response);
        verify(invitationRepository).save(any(BattleInvitation.class));
        verify(notificationRepository).save(any(Notification.class));
        verify(notificationWebSocketHandler).sendNotification(eq(invitee.getUserId()), any(Notification.class));
        verify(mapData).mapOne(invitation, BattleInvitationResponse.class);
    }

    @Test
    public void testSendInvitation_SelfInvitation() {
        invitationRequest.setInviteeId("1"); // Same as inviter
        when(tokenRepository.findByAccessToken("validToken")).thenReturn(Optional.of(token));
        when(userRepository.findById("1")).thenReturn(Optional.of(inviter));
        when(userRepository.findByUserIdAndDeletedAtIsNull("1")).thenReturn(Optional.of(inviter));

        CustomException exception = assertThrows(CustomException.class, () -> {
            battleInvitationService.sendInvitation("validToken", invitationRequest);
        });

        assertEquals("Không thể mời chính mình", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    public void testSendInvitation_TooManyInvitations() {
        when(tokenRepository.findByAccessToken("validToken")).thenReturn(Optional.of(token));
        when(userRepository.findById("1")).thenReturn(Optional.of(inviter));
        when(userRepository.findByUserIdAndDeletedAtIsNull("2")).thenReturn(Optional.of(invitee));
        when(invitationRepository.existsByInviterAndInviteeAndStatus(inviter, invitee, "PENDING")).thenReturn(false);
        when(invitationRepository.findByInviterAndCreatedAtAfter(eq(inviter), any(Instant.class)))
                .thenReturn(List.of(invitation, invitation, invitation));

        CustomException exception = assertThrows(CustomException.class, () -> {
            battleInvitationService.sendInvitation("validToken", invitationRequest);
        });

        assertEquals("Bạn đã gửi quá nhiều lời mời. Vui lòng đợi.", exception.getMessage());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatus());
    }

    @Test
    public void testAcceptInvitation_Success() {
        // Set up token for invitee
        Token inviteeToken = Token.builder().accessToken("validToken").user(invitee).build();
        when(tokenRepository.findByAccessToken("validToken")).thenReturn(Optional.of(inviteeToken));
        when(userRepository.findById("2")).thenReturn(Optional.of(invitee));
        when(invitationRepository.findByInvitationIdAndInvitee("inv1", invitee)).thenReturn(Optional.of(invitation));
        when(battleService.createBattle(any(BattleRequest.class))).thenReturn(battleResponse);
        when(invitationRepository.save(any(BattleInvitation.class))).thenReturn(invitation);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(mapData.mapOne(invitation, BattleInvitationResponse.class)).thenReturn(new BattleInvitationResponse());

        InvitationActionResponse response = battleInvitationService.acceptInvitation("validToken", "inv1");

        assertNotNull(response);
        assertEquals("ACCEPTED", response.getStatus());
        assertEquals("battle1", response.getBattleId());

        ArgumentCaptor<BattleInvitation> invitationCaptor = ArgumentCaptor.forClass(BattleInvitation.class);
        verify(invitationRepository).save(invitationCaptor.capture());
        assertEquals("ACCEPTED", invitationCaptor.getValue().getStatus());
        assertEquals("battle1", invitationCaptor.getValue().getBattleId());

        verify(notificationRepository).save(any(Notification.class));
        verify(notificationWebSocketHandler).sendNotification(eq(inviter.getUserId()), any(Notification.class));
        verify(notificationWebSocketHandler).sendBattleAcceptedMessage(eq(inviter.getUserId()), eq("battle1"));
        verify(notificationWebSocketHandler).sendBattleAcceptedMessage(eq(invitee.getUserId()), eq("battle1"));
    }

    @Test
    public void testAcceptInvitation_Expired() {
        // Set up token for invitee
        Token inviteeToken = Token.builder().accessToken("validToken").user(invitee).build();
        invitation.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES)); // Ensure invitation is expired
        when(tokenRepository.findByAccessToken("validToken")).thenReturn(Optional.of(inviteeToken));
        when(userRepository.findById("2")).thenReturn(Optional.of(invitee));
        when(invitationRepository.findByInvitationIdAndInvitee("inv1", invitee)).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(BattleInvitation.class))).thenReturn(invitation);

        CustomException exception = assertThrows(CustomException.class, () -> {
            battleInvitationService.acceptInvitation("validToken", "inv1");
        });

        assertEquals("Lời mời đã hết hạn", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());

        ArgumentCaptor<BattleInvitation> invitationCaptor = ArgumentCaptor.forClass(BattleInvitation.class);
        verify(invitationRepository).save(invitationCaptor.capture());
        assertEquals("EXPIRED", invitationCaptor.getValue().getStatus());
        verify(invitationRepository).findByInvitationIdAndInvitee("inv1", invitee);
        verify(tokenRepository).findByAccessToken("validToken");
        verify(userRepository).findById("2");
    }

    @Test
    public void testDeclineInvitation_Success() {
        // Set up token for invitee
        Token inviteeToken = Token.builder().accessToken("validToken").user(invitee).build();
        when(tokenRepository.findByAccessToken("validToken")).thenReturn(Optional.of(inviteeToken));
        when(userRepository.findById("2")).thenReturn(Optional.of(invitee));
        when(invitationRepository.findByInvitationIdAndInvitee("inv1", invitee)).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(BattleInvitation.class))).thenReturn(invitation);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(mapData.mapOne(invitation, BattleInvitationResponse.class)).thenReturn(new BattleInvitationResponse());

        InvitationActionResponse response = battleInvitationService.declineInvitation("validToken", "inv1");

        assertNotNull(response);
        assertEquals("DECLINED", response.getStatus());

        ArgumentCaptor<BattleInvitation> invitationCaptor = ArgumentCaptor.forClass(BattleInvitation.class);
        verify(invitationRepository).save(invitationCaptor.capture());
        assertEquals("DECLINED", invitationCaptor.getValue().getStatus());

        verify(notificationRepository).save(any(Notification.class));
        verify(notificationWebSocketHandler).sendNotification(eq(inviter.getUserId()), any(Notification.class));
    }

    @Test
    public void testGetPendingInvitations_Success() {
        // Set up token for invitee
        Token inviteeToken = Token.builder().accessToken("validToken").user(invitee).build();
        when(tokenRepository.findByAccessToken("validToken")).thenReturn(Optional.of(inviteeToken));
        when(userRepository.findById("2")).thenReturn(Optional.of(invitee));
        when(invitationRepository.findByInviteeAndStatusOrderByCreatedAtDesc(invitee, "PENDING"))
                .thenReturn(List.of(invitation));
        when(mapData.mapList(List.of(invitation), BattleInvitationResponse.class))
                .thenReturn(List.of(new BattleInvitationResponse()));

        List<BattleInvitationResponse> response = battleInvitationService.getPendingInvitations("validToken");

        assertNotNull(response);
        assertEquals(1, response.size());
        verify(invitationRepository).findByInviteeAndStatusOrderByCreatedAtDesc(invitee, "PENDING");
        verify(mapData).mapList(List.of(invitation), BattleInvitationResponse.class);
    }

    @Test
    public void testGetSentInvitations_Success() {
        when(tokenRepository.findByAccessToken("validToken")).thenReturn(Optional.of(token));
        when(userRepository.findById("1")).thenReturn(Optional.of(inviter));
        when(invitationRepository.findByInviterOrderByCreatedAtDesc(inviter)).thenReturn(List.of(invitation));
        when(mapData.mapList(List.of(invitation), BattleInvitationResponse.class))
                .thenReturn(List.of(new BattleInvitationResponse()));

        List<BattleInvitationResponse> response = battleInvitationService.getSentInvitations("validToken");

        assertNotNull(response);
        assertEquals(1, response.size());
        verify(invitationRepository).findByInviterOrderByCreatedAtDesc(inviter);
        verify(mapData).mapList(List.of(invitation), BattleInvitationResponse.class);
    }

    @Test
    public void testCancelInvitation_Success() {
        when(tokenRepository.findByAccessToken("validToken")).thenReturn(Optional.of(token));
        when(userRepository.findById("1")).thenReturn(Optional.of(inviter));
        when(invitationRepository.findById("inv1")).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(BattleInvitation.class))).thenReturn(invitation);
        when(mapData.mapOne(invitation, BattleInvitationResponse.class)).thenReturn(new BattleInvitationResponse());

        InvitationActionResponse response = battleInvitationService.cancelInvitation("validToken", "inv1");

        assertNotNull(response);
        assertEquals("CANCELLED", response.getStatus());

        ArgumentCaptor<BattleInvitation> invitationCaptor = ArgumentCaptor.forClass(BattleInvitation.class);
        verify(invitationRepository).save(invitationCaptor.capture());
        assertEquals("CANCELLED", invitationCaptor.getValue().getStatus());
    }

    @Test
    public void testCancelInvitation_NotInviter() {
        User unauthorizedUser = User.builder().userId("3").build();
        Token unauthorizedToken = Token.builder().accessToken("otherToken").user(unauthorizedUser).build();

        when(tokenRepository.findByAccessToken("otherToken")).thenReturn(Optional.of(unauthorizedToken));
        when(userRepository.findById("3")).thenReturn(Optional.of(unauthorizedUser));
        when(invitationRepository.findById("inv1")).thenReturn(Optional.of(invitation));

        CustomException exception = assertThrows(CustomException.class, () -> {
            battleInvitationService.cancelInvitation("otherToken", "inv1");
        });

        assertEquals("Bạn không có quyền hủy lời mời này", exception.getMessage());
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    public void testCleanupExpiredInvitations_Success() {
        // Ensure the invitation is expired
        invitation.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        when(invitationRepository.findByStatusAndExpiresAtBefore(eq("PENDING"), any(Instant.class)))
                .thenReturn(List.of(invitation));
        when(invitationRepository.save(any(BattleInvitation.class))).thenReturn(invitation);

        battleInvitationService.cleanupExpiredInvitations();

        ArgumentCaptor<BattleInvitation> invitationCaptor = ArgumentCaptor.forClass(BattleInvitation.class);
        verify(invitationRepository).save(invitationCaptor.capture());
        assertEquals("EXPIRED", invitationCaptor.getValue().getStatus());
        verify(invitationRepository).findByStatusAndExpiresAtBefore(eq("PENDING"), any(Instant.class));
    }

    @Test
    public void testCleanupExpiredInvitations_NoExpiredInvitations() {
        when(invitationRepository.findByStatusAndExpiresAtBefore("PENDING", Instant.now()))
                .thenReturn(Collections.emptyList());

        battleInvitationService.cleanupExpiredInvitations();

        verify(invitationRepository).findByStatusAndExpiresAtBefore("PENDING", Instant.now());
        verify(invitationRepository, never()).save(any(BattleInvitation.class));
    }
}