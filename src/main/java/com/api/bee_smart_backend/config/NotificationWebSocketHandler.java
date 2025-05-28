package com.api.bee_smart_backend.config;

import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.model.Notification;
import com.api.bee_smart_backend.model.User;
import com.api.bee_smart_backend.repository.NotificationRepository;
import com.api.bee_smart_backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Store active sessions by userId
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = (String) session.getAttributes().get("userId");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("Không tìm thấy người dùng với ID: " + userId, HttpStatus.NOT_FOUND));
        if (user != null) {
            userSessions.put(userId, session);
            log.info("User {} connected to notification websocket", userId);

            // Send unread notifications on connect
            List<Notification> unreadNotifications = notificationRepository.findByUserAndReadFalse(user);
            if (!unreadNotifications.isEmpty()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                        "type", "UNREAD_NOTIFICATIONS",
                        "notifications", unreadNotifications
                ))));
            }
        } else {
            session.close(CloseStatus.BAD_DATA.withReason("Missing user ID"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            userSessions.remove(userId);
            log.info("User {} disconnected from notification websocket", userId);
        }
    }

    public void sendNotification(String userId, Notification notification) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                        "type", "NEW_NOTIFICATION",
                        "notification", notification
                ))));
                log.info("Notification sent to user {}", userId);
            } catch (IOException e) {
                log.error("Error sending notification to user {}", userId, e);
            }
        }
    }

    public void markAsRead(String userId, String notificationId) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                        "type", "NOTIFICATION_READ",
                        "notificationId", notificationId
                ))));
            } catch (IOException e) {
                log.error("Error marking notification as read for user {}", userId, e);
            }
        }
    }

    public void sendInvitationUpdate(String userId, String type, Object data) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                        "type", type,
                        "data", data
                ))));
                log.info("Invitation update sent to user {}: {}", userId, type);
            } catch (IOException e) {
                log.error("Error sending invitation update to user {}", userId, e);
            }
        }
    }
}