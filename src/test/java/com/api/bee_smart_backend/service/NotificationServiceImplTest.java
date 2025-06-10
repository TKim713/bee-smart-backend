package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.config.NotificationWebSocketHandler;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.response.NotificationResponse;
import com.api.bee_smart_backend.model.Notification;
import com.api.bee_smart_backend.model.Token;
import com.api.bee_smart_backend.model.User;
import com.api.bee_smart_backend.repository.NotificationRepository;
import com.api.bee_smart_backend.repository.TokenRepository;
import com.api.bee_smart_backend.repository.UserRepository;
import com.api.bee_smart_backend.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationWebSocketHandler notificationWebSocketHandler;

    @Mock
    private MapData mapData;

    private User user;
    private Token token;
    private Notification notification;
    private NotificationResponse notificationResponse;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId("user1")
                .username("User1")
                .build();

        token = Token.builder()
                .accessToken("valid-token")
                .user(user)
                .build();

        notification = Notification.builder()
                .notificationId("notif1")
                .user(user)
                .title("Test Notification")
                .message("This is a test")
                .link("/test")
                .type("INFO")
                .read(false)
                .createdAt(Instant.now())
                .build();

        notificationResponse = new NotificationResponse();
        notificationResponse.setNotificationId("notif1");
        notificationResponse.setTitle("Test Notification");
        notificationResponse.setMessage("This is a test");
        notificationResponse.setLink("/test");
        notificationResponse.setType("INFO");
        notificationResponse.setRead(false);
    }

    @Test
    void getNotifications_ValidToken_ShouldReturnNotifications() {
        when(tokenRepository.findByAccessToken("valid-token")).thenReturn(Optional.of(token));
        when(userRepository.findById("user1")).thenReturn(Optional.of(user));
        when(notificationRepository.findByUserAndDeletedAtIsNullOrderByCreatedAtDesc(user))
                .thenReturn(List.of(notification));
        when(mapData.mapList(List.of(notification), NotificationResponse.class))
                .thenReturn(List.of(notificationResponse));

        List<NotificationResponse> responses = notificationService.getNotifications("valid-token");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("notif1", responses.get(0).getNotificationId());
        verify(notificationWebSocketHandler).sendNotification(eq("user1"), eq(notification));
        verify(notificationRepository).findByUserAndDeletedAtIsNullOrderByCreatedAtDesc(user);
        verify(mapData).mapList(List.of(notification), NotificationResponse.class);
    }

    @Test
    void getNotifications_InvalidToken_ShouldThrowUnauthorized() {
        when(tokenRepository.findByAccessToken("invalid-token")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> notificationService.getNotifications("invalid-token"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Invalid token", exception.getReason());
        verifyNoInteractions(notificationWebSocketHandler, notificationRepository, mapData);
    }

    @Test
    void getNotifications_UserNotFound_ShouldThrowCustomException() {
        when(tokenRepository.findByAccessToken("valid-token")).thenReturn(Optional.of(token));
        when(userRepository.findById("user1")).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class,
                () -> notificationService.getNotifications("valid-token"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Không tìm thấy người dùng với ID: user1", exception.getMessage());
        verifyNoInteractions(notificationWebSocketHandler, notificationRepository, mapData);
    }

    @Test
    void createNotification_ValidInput_ShouldSaveAndSendNotification() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        notificationService.createNotification(user, "Test Title", "Test Message", "/test", "INFO");

        verify(notificationRepository).save(any(Notification.class));
        verify(notificationWebSocketHandler).sendNotification(eq("user1"), any(Notification.class));
        verify(notificationRepository).save(argThat(n ->
                n.getUser().equals(user) &&
                        n.getTitle().equals("Test Title") &&
                        n.getMessage().equals("Test Message") &&
                        n.getLink().equals("/test") &&
                        n.getType().equals("INFO") &&
                        !n.isRead()
        ));
    }

    @Test
    void markAsRead_ValidNotification_ShouldMarkAsRead() {
        when(tokenRepository.findByAccessToken("valid-token")).thenReturn(Optional.of(token));
        when(notificationRepository.findById("notif1")).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        boolean result = notificationService.markAsRead("valid-token", "notif1");

        assertTrue(result);
        verify(notificationRepository).save(argThat(n -> n.isRead() && n.getUpdatedAt() != null));
        verify(notificationWebSocketHandler).markAsRead("user1", "notif1");
    }

    @Test
    void markAsRead_NotificationNotFound_ShouldThrowNotFound() {
        when(tokenRepository.findByAccessToken("valid-token")).thenReturn(Optional.of(token));
        when(notificationRepository.findById("notif1")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> notificationService.markAsRead("valid-token", "notif1"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Notification not found", exception.getReason());
        verifyNoInteractions(notificationWebSocketHandler);
    }

    @Test
    void markAsRead_UnauthorizedUser_ShouldThrowForbidden() {
        User otherUser = User.builder().userId("user2").username("User2").build();
        notification.setUser(otherUser);

        when(tokenRepository.findByAccessToken("valid-token")).thenReturn(Optional.of(token));
        when(notificationRepository.findById("notif1")).thenReturn(Optional.of(notification));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> notificationService.markAsRead("valid-token", "notif1"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Access denied", exception.getReason());
        verifyNoInteractions(notificationWebSocketHandler);
    }

    @Test
    void markAllAsRead_ValidToken_ShouldMarkAllUnreadAsRead() {
        Notification unread1 = Notification.builder()
                .notificationId("notif1")
                .user(user)
                .read(false)
                .build();
        Notification unread2 = Notification.builder()
                .notificationId("notif2")
                .user(user)
                .read(false)
                .build();

        when(tokenRepository.findByAccessToken("valid-token")).thenReturn(Optional.of(token));
        when(userRepository.findById("user1")).thenReturn(Optional.of(user));
        when(notificationRepository.findByUserAndReadFalse(user)).thenReturn(List.of(unread1, unread2));
        when(notificationRepository.save(any(Notification.class))).thenReturn(unread1).thenReturn(unread2);

        int count = notificationService.markAllAsRead("valid-token");

        assertEquals(2, count);
        verify(notificationRepository, times(2)).save(argThat(n -> n.isRead() && n.getUpdatedAt() != null));
        verify(notificationWebSocketHandler).markAsRead("user1", "notif1");
        verify(notificationWebSocketHandler).markAsRead("user1", "notif2");
    }

    @Test
    void markAllAsRead_NoUnreadNotifications_ShouldReturnZero() {
        when(tokenRepository.findByAccessToken("valid-token")).thenReturn(Optional.of(token));
        when(userRepository.findById("user1")).thenReturn(Optional.of(user));
        when(notificationRepository.findByUserAndReadFalse(user)).thenReturn(List.of());

        int count = notificationService.markAllAsRead("valid-token");

        assertEquals(0, count);
        verify(notificationRepository, never()).save(any());
        verifyNoInteractions(notificationWebSocketHandler);
    }

    @Test
    void deleteAllNotifications_ValidToken_ShouldDeleteAllNotifications() {
        Notification notif1 = Notification.builder()
                .notificationId("notif1")
                .user(user)
                .build();
        Notification notif2 = Notification.builder()
                .notificationId("notif2")
                .user(user)
                .build();

        when(tokenRepository.findByAccessToken("valid-token")).thenReturn(Optional.of(token));
        when(userRepository.findById("user1")).thenReturn(Optional.of(user));
        when(notificationRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(notif1, notif2));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notif1).thenReturn(notif2);

        int count = notificationService.deleteAllNotifications("valid-token");

        assertEquals(2, count);
        verify(notificationRepository, times(2)).save(argThat(n -> n.getDeletedAt() != null));
        verify(notificationWebSocketHandler).sendNotificationUpdate(eq("user1"), eq("ALL_NOTIFICATIONS_DELETED"),
                eq(Map.of("count", 2)));
    }

    @Test
    void deleteAllNotifications_NoNotifications_ShouldReturnZero() {
        when(tokenRepository.findByAccessToken("valid-token")).thenReturn(Optional.of(token));
        when(userRepository.findById("user1")).thenReturn(Optional.of(user));
        when(notificationRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of());

        int count = notificationService.deleteAllNotifications("valid-token");

        assertEquals(0, count);
        verify(notificationRepository, never()).save(any());
        verify(notificationWebSocketHandler).sendNotificationUpdate(eq("user1"), eq("ALL_NOTIFICATIONS_DELETED"),
                eq(Map.of("count", 0)));
    }

    @Test
    void getUnreadCount_ValidToken_ShouldReturnUnreadCount() {
        when(tokenRepository.findByAccessToken("valid-token")).thenReturn(Optional.of(token));
        when(userRepository.findById("user1")).thenReturn(Optional.of(user));
        when(notificationRepository.countByUserAndReadFalseAndDeletedAtIsNull(user)).thenReturn(3);

        int count = notificationService.getUnreadCount("valid-token");

        assertEquals(3, count);
        verify(notificationWebSocketHandler).sendNotificationUpdate(eq("user1"), eq("UNREAD_COUNT"),
                eq(Map.of("count", 3)));
    }

    @Test
    void getUnreadCount_InvalidToken_ShouldThrowUnauthorized() {
        when(tokenRepository.findByAccessToken("invalid-token")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> notificationService.getUnreadCount("invalid-token"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Invalid token", exception.getReason());
        verifyNoInteractions(notificationWebSocketHandler, notificationRepository);
    }
}