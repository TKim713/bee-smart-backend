package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.response.NotificationResponse;

import java.util.List;

public interface NotificationService {
    List<NotificationResponse> getNotifications(String token);

    boolean markAsRead(String token, String notificationId);

    int markAllAsRead(String token);

    int deleteAllNotifications(String token);

    int getUnreadCount(String token);
}
