package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.response.NotificationResponse;
import com.api.bee_smart_backend.helper.response.ResponseObject;
import com.api.bee_smart_backend.model.Notification;
import com.api.bee_smart_backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ResponseObject<List<NotificationResponse>>> getNotifications(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            List<NotificationResponse> notifications = notificationService.getNotifications(token);
            return ResponseEntity.ok(new ResponseObject<>(HttpStatus.OK.value(), "Notifications retrieved", notifications));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), Collections.emptyList()));
        }
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ResponseObject<Map<String, Object>>> markAsRead(
            @PathVariable String notificationId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            boolean result = notificationService.markAsRead(token, notificationId);
            return ResponseEntity.ok(new ResponseObject<>(HttpStatus.OK.value(), "Notification marked as read", Map.of("success", result)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }

    @PutMapping("/read-all")
    public ResponseEntity<ResponseObject<Map<String, Object>>> markAllAsRead(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            int count = notificationService.markAllAsRead(token);
            return ResponseEntity.ok(new ResponseObject<>(HttpStatus.OK.value(), "All notifications marked as read", Map.of("success", true, "count", count)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }

    @DeleteMapping("/delete-all")
    public ResponseEntity<ResponseObject<Map<String, Object>>> deleteAllNotifications(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            int count = notificationService.deleteAllNotifications(token);
            return ResponseEntity.ok(new ResponseObject<>(HttpStatus.OK.value(), "All notifications deleted", Map.of("success", true, "count", count)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }
}
