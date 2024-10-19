package com.api.bee_smart_backend.service;

public interface EmailService {
    void sendEmail(String to, String subject, String token, String username);
}
