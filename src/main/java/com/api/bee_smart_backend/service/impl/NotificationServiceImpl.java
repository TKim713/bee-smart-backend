package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.config.NotificationWebSocketHandler;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.response.NotificationResponse;
import com.api.bee_smart_backend.model.Notification;
import com.api.bee_smart_backend.model.User;
import com.api.bee_smart_backend.repository.NotificationRepository;
import com.api.bee_smart_backend.repository.TokenRepository;
import com.api.bee_smart_backend.repository.UserRepository;
import com.api.bee_smart_backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private final NotificationRepository notificationRepository;
    @Autowired
    private final TokenRepository tokenRepository;
    @Autowired
    private final UserRepository userRepository;
    private final NotificationWebSocketHandler notificationWebSocketHandler;

    private final MapData mapData;

    private String getUserIdFromToken(String token) {
        return tokenRepository.findByAccessToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"))
                .getUser().getUserId();
    }

    @Override
    public List<NotificationResponse> getNotifications(String token) {
        String userId = getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("Không tìm thấy người dùng với ID: " + userId, HttpStatus.NOT_FOUND));
        // Use the new method that filters out deleted notifications
        List<Notification> notificationList = notificationRepository.findByUserAndDeletedAtIsNullOrderByCreatedAtDesc(user);
        return mapData.mapList(notificationList, NotificationResponse.class);
    }

    @Override
    public boolean markAsRead(String token, String notificationId) {
        String userId = getUserIdFromToken(token);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!notification.getUser().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        notification.setRead(true);
        notification.setUpdatedAt(Instant.now());
        notificationRepository.save(notification);

        notificationWebSocketHandler.markAsRead(userId, notificationId);
        return true;
    }

    @Override
    public int markAllAsRead(String token) {
        String userId = getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("Không tìm thấy người dùng với ID: " + userId, HttpStatus.NOT_FOUND));

        List<Notification> unread = notificationRepository.findByUserAndReadFalse(user);
        for (Notification n : unread) {
            n.setRead(true);
            n.setUpdatedAt(Instant.now());
            notificationRepository.save(n);
            notificationWebSocketHandler.markAsRead(userId, n.getNotificationId());
        }
        return unread.size();
    }

    @Override
    public int deleteAllNotifications(String token) {
        String userId = getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("Không tìm thấy người dùng với ID: " + userId, HttpStatus.NOT_FOUND));

        List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        for (Notification n : notifications) {
            n.setDeletedAt(Instant.now());
            notificationRepository.save(n);
        }
        return notifications.size();
    }

    @Override
    public int getUnreadCount(String token) {
        String userId = getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("Không tìm thấy người dùng với ID: " + userId, HttpStatus.NOT_FOUND));

        return notificationRepository.countByUserAndReadFalseAndDeletedAtIsNull(user);
    }
}
