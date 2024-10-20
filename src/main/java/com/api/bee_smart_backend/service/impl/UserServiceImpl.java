package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.enums.Role;
import com.api.bee_smart_backend.helper.enums.TokenType;
import com.api.bee_smart_backend.helper.request.CreateUserRequest;
import com.api.bee_smart_backend.helper.response.CreateUserResponse;
import com.api.bee_smart_backend.model.Token;
import com.api.bee_smart_backend.model.User;
import com.api.bee_smart_backend.repository.TokenRepository;
import com.api.bee_smart_backend.repository.UserRepository;
import com.api.bee_smart_backend.service.EmailService;
import com.api.bee_smart_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    @Autowired
    private final UserRepository userRepository;
    @Autowired
    private final TokenRepository tokenRepository;
    @Autowired
    private final EmailService emailService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    private final MapData mapData;

    private LocalDateTime now = LocalDateTime.now();

    @Override
    public CreateUserResponse createUser(CreateUserRequest userRequest) {
        // Mã hóa mật khẩu
        String password = passwordEncoder.encode(userRequest.getPassword());

        // Tạo đối tượng User
        User user = User.builder()
                .username(userRequest.getUsername())
                .email(userRequest.getEmail())
                .password(password)
                .role(Role.valueOf(userRequest.getRole()))
                .create_at(Timestamp.valueOf(now))
                .build();

        User savedUser = userRepository.save(user);

        // Tạo token xác thực
        String tokenStr = UUID.randomUUID().toString();
        Token token = new Token();
        token.setToken(tokenStr);
        token.setTokenType(TokenType.BEARER);
        token.setExpired(false);
        token.setRevoked(false);
        token.setUser(savedUser);
        token.setCreate_at(Timestamp.valueOf(now));
        tokenRepository.save(token);

        // Gửi email xác thực với tên người dùng
        emailService.sendEmail(user.getEmail(), "🌟 Xác Thực Email của Bạn cho Bee Smart! 🌟", tokenStr, savedUser.getUsername());

        CreateUserResponse response = mapData.mapOne(savedUser, CreateUserResponse.class);
        response.setPassword(user.getPassword());
        response.setRole(user.getRole().toString());
        return response;
    }


    // Method xác thực email
    public String verifyEmail(String tokenStr) {
        Token token = tokenRepository.findByToken(tokenStr);
        if (token != null && !token.isExpired() && !token.isRevoked()) {
            User user = token.getUser();
            user.setEnabled(true);  // Kích hoạt tài khoản user
            userRepository.save(user);

            token.setExpired(true); // Đánh dấu token đã hết hạn sau khi xác thực
            tokenRepository.save(token);

            return "Email verified successfully!";
        } else {
            return "Invalid or expired verification link!";
        }
    }
}
