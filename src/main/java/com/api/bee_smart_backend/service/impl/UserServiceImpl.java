package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.enums.Role;
import com.api.bee_smart_backend.helper.enums.TokenType;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.CreateUserRequest;
import com.api.bee_smart_backend.helper.response.CreateUserResponse;
import com.api.bee_smart_backend.helper.response.UserResponse;
import com.api.bee_smart_backend.model.Token;
import com.api.bee_smart_backend.model.User;
import com.api.bee_smart_backend.repository.TokenRepository;
import com.api.bee_smart_backend.repository.UserRepository;
import com.api.bee_smart_backend.service.EmailService;
import com.api.bee_smart_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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

    private final Instant now = Instant.now();

    @Override
    public CreateUserResponse createUser(CreateUserRequest userRequest) {
        // Check if username already exists
        Optional<User> existingUsername = userRepository.findByUsername(userRequest.getUsername());
        if (existingUsername.isPresent()) {
            throw new CustomException("Username đã tồn tại", HttpStatus.CONFLICT);
        }

        // Check if email already exists
        Optional<User> existingUserEmail = userRepository.findByEmail(userRequest.getEmail());
        if (existingUserEmail.isPresent()) {
            throw new CustomException("Email đã tồn tại", HttpStatus.CONFLICT);
        }

        // Mã hóa mật khẩu
        String password = passwordEncoder.encode(userRequest.getPassword());

        // Tạo đối tượng User
        User user = User.builder()
                .username(userRequest.getUsername())
                .email(userRequest.getEmail())
                .password(password)
                .role(Role.valueOf(userRequest.getRole()))
                .enabled(false)  // Default to false until verified
                .active(true)
                .createdAt(now)
                .build();

        User savedUser = userRepository.save(user);

        // Tạo token xác thực
        String tokenStr = UUID.randomUUID().toString();

        Token token = Token.builder()
                .accessToken(tokenStr)
                .tokenType(TokenType.valueOf(TokenType.BEARER.name()))
                .expired(false)
                .revoked(false)
                .user(savedUser)
                .createdAt(now)
                .build();

        Token savedToken = tokenRepository.save(token);

        // Gửi email xác thực với tên người dùng
        emailService.sendEmail(user.getEmail(), "🌟 Xác Thực Email của Bạn cho Bee Smart! 🌟", tokenStr, savedUser.getUsername());

        CreateUserResponse response = mapData.mapOne(savedUser, CreateUserResponse.class);
        response.setToken(savedToken.getAccessToken());
        return response;
    }

    // Method xác thực email
    public String verifyEmail(String tokenStr) {
        Token token = tokenRepository.findByAccessToken(tokenStr);
        if (token != null && !token.isExpired() && !token.isRevoked()) {
            User user = token.getUser();
            user.setEnabled(true);  // Kích hoạt tài khoản user
            userRepository.save(user);

            token.setExpired(true); // Đánh dấu token đã hết hạn sau khi xác thực
            token.setRevoked(true);
            token.setUpdatedAt(now);
            token.setDeletedAt(now);
            tokenRepository.save(token);

            return "Xác thực email thành công!";
        } else {
            return "Liên kết xác minh không hợp lệ hoặc đã hết hạn!";
        }
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAllActive().stream()
                .map(user -> UserResponse.builder()
                        .userId(user.getUserId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .enabled(user.isEnabled())
                        .active(user.isActive())
                        .createdAt(user.getCreatedAt())
                        .updatedAt(user.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public void deleteUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("Người dùng không tồn tại", HttpStatus.NOT_FOUND));
        user.setDeletedAt(now);
        userRepository.save(user);
    }

    @Override
    public void deactivateUser(String userId, boolean activeStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("Người dùng không tồn tại", HttpStatus.NOT_FOUND));

        if (user.isActive() == activeStatus) {
            String action = activeStatus ? "kích hoạt" : "vô hiệu hóa";
            throw new CustomException("Người dùng đã được " + action + " trước đó", HttpStatus.BAD_REQUEST);
        }

        user.setActive(activeStatus);
        user.setUpdatedAt(now);
        userRepository.save(user);
    }
}