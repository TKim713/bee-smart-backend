package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.enums.Role;
import com.api.bee_smart_backend.helper.enums.TokenType;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.CreateStudentRequest;
import com.api.bee_smart_backend.helper.request.CreateUserRequest;
import com.api.bee_smart_backend.helper.response.CreateStudentResponse;
import com.api.bee_smart_backend.helper.response.CreateUserResponse;
import com.api.bee_smart_backend.helper.response.UserResponse;
import com.api.bee_smart_backend.model.*;
import com.api.bee_smart_backend.repository.*;
import com.api.bee_smart_backend.service.EmailService;
import com.api.bee_smart_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
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
    private final ParentRepository parentRepository;
    @Autowired
    private final StudentRepository studentRepository;
    @Autowired
    private final CustomerRepository customerRepository;
    @Autowired
    private final GradeRepository gradeRepository;
    @Autowired
    private final EmailService emailService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    private final MapData mapData;

    private final Instant now = Instant.now();

    @Override
    public CreateUserResponse createUser(CreateUserRequest userRequest) {
        Optional<User> existingUsername = userRepository.findByUsername(userRequest.getUsername());
        if (existingUsername.isPresent()) {
            throw new CustomException("Username đã tồn tại", HttpStatus.CONFLICT);
        }

        Optional<User> existingUserEmail = userRepository.findByEmail(userRequest.getEmail());
        if (existingUserEmail.isPresent()) {
            throw new CustomException("Email đã tồn tại", HttpStatus.CONFLICT);
        }

        String password = passwordEncoder.encode(userRequest.getPassword());

        User user = User.builder()
                .username(userRequest.getUsername())
                .email(userRequest.getEmail())
                .password(password)
                .role(Role.valueOf(userRequest.getRole()))
                .enabled(false)
                .active(true)
                .createdAt(now)
                .build();

        User savedUser = userRepository.save(user);
        Role role = Role.valueOf(userRequest.getRole());

        if (role == Role.PARENT) {
            Parent parent = Parent.builder()
                    .fullName(userRequest.getFullName())
                    .district("")
                    .city("")
                    .dateOfBirth(LocalDate.of(2000, 1, 1))
                    .phone("")
                    .address("")
                    .user(savedUser)
                    .createdAt(now)
                    .build();

            customerRepository.save(parent);
        } else if (role == Role.STUDENT) {
            Student student = Student.builder()
                    .fullName(userRequest.getFullName())
                    .district("")
                    .city("")
                    .dateOfBirth(LocalDate.of(2000, 1, 1))
                    .phone("")
                    .address("")
                    .user(savedUser)
                    .grade("")
                    .className("")
                    .createdAt(now)
                    .build();

            customerRepository.save(student);
        }

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

        emailService.sendEmail(user.getEmail(), "🌟 Xác Thực Email của Bạn cho Bee Smart! 🌟", tokenStr, savedUser.getUsername());

        CreateUserResponse response = mapData.mapOne(savedUser, CreateUserResponse.class);
        response.setToken(savedToken.getAccessToken());
        return response;
    }

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

    @Override
    public CreateStudentResponse createStudentByParent(String parentId, CreateStudentRequest studentRequest) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new CustomException("Parent not found", HttpStatus.NOT_FOUND));

        if (studentRequest.getGrade() == null || studentRequest.getGrade().isBlank()) {
            throw new CustomException("Grade is required when creating a student account", HttpStatus.BAD_REQUEST);
        }

        String password = passwordEncoder.encode(studentRequest.getPassword());
        User user = User.builder()
                .username(studentRequest.getUsername())
                .password(password)
                .role(Role.valueOf(studentRequest.getRole()))
                .enabled(true)
                .active(true)
                .createdAt(now)
                .build();

        User savedUser = userRepository.save(user);

        Student student = Student.builder()
                .fullName(studentRequest.getFullName())
                .district("")
                .city("")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .phone("")
                .address("")
                .user(savedUser)
                .grade(studentRequest.getGrade())
                .parent(parent)
                .className("")
                .build();

        studentRepository.save(student);

        return mapData.mapOne(savedUser, CreateStudentResponse.class);
    }
}