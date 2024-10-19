package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    private static final String BASE_URL = "http://localhost:8080/api/auth/verify?token="; // Define the base URL here

    @Override
    public void sendEmail(String to, String subject, String token, String username) {
        String body = generateEmailBody(token, username);

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            javaMailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email", e);
        }
    }

    private String generateEmailBody(String token, String username) {
        return String.format(
                "<p>Kính gửi <strong>%s</strong>,</p>" +
                        "<p>Cảm ơn bạn đã tham gia gia đình <strong>Bee Smart</strong>! Chúng tôi rất vui mừng chào đón bạn trong hành trình thú vị để khám phá thế giới toán học.</p>" +
                        "<p>Để bắt đầu, vui lòng xác thực địa chỉ email của bạn bằng cách nhấp vào liên kết dưới đây:</p>" +
                        "<p>👉 <a href=\"%s%s\">Xác thực Email của Bạn</a></p>" +
                        "<p>Bước này giúp chúng tôi đảm bảo rằng bạn có thể nhận được thông tin quan trọng về tiến trình học tập và các bài học sắp tới!</p>" +
                        "<p>Nếu bạn có bất kỳ câu hỏi nào hoặc cần hỗ trợ, vui lòng liên hệ với đội ngũ hỗ trợ của chúng tôi tại <strong>support@beesmart.com</strong>.</p>" +
                        "<p>Cảm ơn bạn đã trở thành một phần của cộng đồng chúng tôi! Chúng tôi mong muốn giúp con bạn trở thành một bậc thầy về toán học!</p>" +
                        "<p>Chúc bạn học tập vui vẻ!</p>" +
                        "<p>Trân trọng,<br>Đội Ngũ <strong>Bee Smart</strong><br><a href=\"http://www.beesmart.com\">www.beesmart.com</a></p>",
                username, BASE_URL, token
        );
    }
}
