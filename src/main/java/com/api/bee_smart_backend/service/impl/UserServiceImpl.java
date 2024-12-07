package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.JwtTokenUtil;
import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.enums.Role;
import com.api.bee_smart_backend.helper.enums.TokenType;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.ChangePasswordRequest;
import com.api.bee_smart_backend.helper.request.CreateStudentRequest;
import com.api.bee_smart_backend.helper.request.CreateUserRequest;
import com.api.bee_smart_backend.helper.response.*;
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
    private final StatisticRepository statisticRepository;
    @Autowired
    private final EmailService emailService;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    private final MapData mapData;

    private final Instant now = Instant.now();

    @Override
    public CreateUserResponse createUser(CreateUserRequest userRequest) {
        Optional<User> existingUsername = userRepository.findByUsernameAndDeletedAtIsNull(userRequest.getUsername());
        if (existingUsername.isPresent()) {
            throw new CustomException("Username đã tồn tại", HttpStatus.CONFLICT);
        }

        Optional<User> existingUserEmail = userRepository.findByEmailAndDeletedAtIsNull(userRequest.getEmail());
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
                .tokenType(TokenType.VERIFY)
                .expired(false)
                .revoked(false)
                .user(savedUser)
                .createdAt(now)
                .build();

        Token savedToken = tokenRepository.save(token);

        emailService.sendEmailWithTemplate(
                user.getEmail(),
                "🌟 Xác Thực Email của Bạn cho Bee Smart! 🌟",
                EmailServiceImpl.VERIFICATION_EMAIL_TEMPLATE,
                savedUser.getUsername(),
                EmailServiceImpl.BASE_URL + tokenStr
        );

        CreateUserResponse response = mapData.mapOne(savedUser, CreateUserResponse.class);
        response.setToken(savedToken.getAccessToken());
        return response;
    }

    @Override
    public void verifyEmail(String tokenStr) {
        Token token = tokenRepository.findByAccessToken(tokenStr)
                .orElseThrow(() -> new CustomException("Token not found", HttpStatus.NOT_FOUND));
        if (!token.isExpired() && !token.isRevoked()) {
            User user = token.getUser();
            user.setEnabled(true);
            userRepository.save(user);

            token.setExpired(true);
            token.setRevoked(true);
            token.setUpdatedAt(now);
            token.setDeletedAt(now);
            tokenRepository.save(token);

            Statistic statistic = Statistic.builder()
                    .user(user)
                    .numberOfQuestionsAnswered(0)
                    .timeSpentLearning(0)
                    .numberOfQuizzesDone(0)
                    .timeSpentDoingQuizzes(0).build();

            statisticRepository.save(statistic);

        } else {
            throw  new CustomException("Liên kết xác thực hết hạn hoặc không hợp lệ", HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public boolean changePassword(String tokenStr, ChangePasswordRequest changePasswordRequest) {
        Token token = tokenRepository.findByAccessToken(tokenStr)
                .orElseThrow(() -> new CustomException("Token not found", HttpStatus.NOT_FOUND));
        String username = jwtTokenUtil.getUsernameFromToken(token.getAccessToken());
        User user = userRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        if (!passwordEncoder.matches(changePasswordRequest.getOldPassword(), user.getPassword())) {
            throw new CustomException("Mật khẩu cũ không trùng khớp", HttpStatus.BAD_REQUEST);
        }

        if (changePasswordRequest.getOldPassword().equals(changePasswordRequest.getNewPassword())) {
            throw new CustomException("Mật khẩu mới không được trùng với mật khẩu cũ", HttpStatus.BAD_REQUEST);
        }

        String encodedNewPassword = passwordEncoder.encode(changePasswordRequest.getNewPassword());
        user.setPassword(encodedNewPassword);
        userRepository.save(user);

        return true;
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> UserResponse.builder()
                        .userId(user.getUserId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .enabled(user.isEnabled())
                        .active(user.isActive())
                        .createdAt(user.getCreatedAt())
                        .updatedAt(user.getUpdatedAt())
                        .deletedAt(user.getDeletedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public UserCustomerResponse getUserInfo(String jwtToken) {
        Token token = tokenRepository.findByAccessToken(jwtToken)
                .orElseThrow(() -> new CustomException("Token not found", HttpStatus.NOT_FOUND));
        User user = userRepository.findById(token.getUser().getUserId())
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        Customer customer = customerRepository.findByUserAndDeletedAtIsNull(token.getUser())
                .orElseThrow(() -> new CustomException("Customer not found for user", HttpStatus.NOT_FOUND));

        UserCustomerResponse response = UserCustomerResponse.builder()
                .fullName(customer.getFullName())
                .username(user.getUsername())
                .role(user.getRole().name())
                .district(customer.getDistrict())
                .city(customer.getCity())
                .dateOfBirth(customer.getDateOfBirth())
                .phone(customer.getPhone())
                .email(user.getEmail())
                .address(customer.getAddress())
                .build();

        if (customer instanceof Student student) {
            response.setGrade(student.getGrade());
            response.setClassName(student.getClassName());
            response.setSchool(student.getSchool());
        }

        return response;
    }

    @Override
    public void deleteUsersByIds(List<String> userIds) {
        Instant now = Instant.now();

        for (String userId : userIds) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException("Người dùng không tồn tại", HttpStatus.NOT_FOUND));

            Customer customer = customerRepository.findByUserAndDeletedAtIsNull(user)
                    .orElseThrow(() -> new CustomException("Khách hàng không tìm thấy cho người dùng: ", HttpStatus.NOT_FOUND));

            user.setDeletedAt(now);
            customer.setDeletedAt(now);

            userRepository.save(user);
            customerRepository.save(customer);
        }
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
                .orElseThrow(() -> new CustomException("Không tìm thấy phụ huynh", HttpStatus.NOT_FOUND));

        Optional<User> existingUsername = userRepository.findByUsernameAndDeletedAtIsNull(studentRequest.getUsername());
        if (existingUsername.isPresent()) {
            throw new CustomException("Username đã tồn tại", HttpStatus.CONFLICT);
        }

        if (studentRequest.getGrade() == null || studentRequest.getGrade().isBlank()) {
            throw new CustomException("Grade is required when creating a student account", HttpStatus.BAD_REQUEST);
        }

        String password = passwordEncoder.encode(studentRequest.getPassword());
        User user = User.builder()
                .username(studentRequest.getUsername())
                .email("")
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
                .school("")
                .build();

        studentRepository.save(student);

        Statistic statistic = Statistic.builder()
                .user(savedUser)
                .numberOfQuestionsAnswered(0)
                .timeSpentLearning(0)
                .numberOfQuizzesDone(0)
                .timeSpentDoingQuizzes(0).build();

        return mapData.mapOne(savedUser, CreateStudentResponse.class);
    }
}