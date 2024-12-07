package com.api.bee_smart_backend.service;

public interface EmailService {
    void sendEmail(String to, String subject, String username);

    void sendEmailWithTemplate(String to, String subject, String template, Object... placeholders);
}
