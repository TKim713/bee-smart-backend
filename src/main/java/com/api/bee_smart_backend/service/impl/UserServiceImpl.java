package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.JwtTokenUtil;
import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.enums.Role;
import com.api.bee_smart_backend.helper.enums.TokenType;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.ChangePasswordRequest;
import com.api.bee_smart_backend.helper.request.CreateStudentRequest;
import com.api.bee_smart_backend.helper.request.CreateUserRequest;
import com.api.bee_smart_backend.helper.request.UserRequest;
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

import java.time.Duration;
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
                    .district(userRequest.getDistrict())
                    .city(userRequest.getCity())
                    .dateOfBirth(LocalDate.of(2000, 1, 1))
                    .phone("")
                    .address("")
                    .user(savedUser)
                    .grade(userRequest.getGrade())
                    .className("")
                    .school(userRequest.getSchool())
                    .createdAt(now)
                    .build();

            customerRepository.save(student);
        }

        Instant tokenExpirationTime = Instant.now().plus(Duration.ofMinutes(10));
        String tokenStr = UUID.randomUUID().toString();

        Token token = Token.builder()
                .accessToken(tokenStr)
                .tokenType(TokenType.VERIFY)
                .expired(false)
                .revoked(false)
                .user(savedUser)
                .expirationTime(tokenExpirationTime)
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
                .orElseThrow(() -> new CustomException("Không tìm thấy token", HttpStatus.NOT_FOUND));
        if (token.getExpirationTime().isAfter(now) && !token.isRevoked()) {
            User user = token.getUser();
            user.setEnabled(true);
            userRepository.save(user);

            token.setExpired(true);
            token.setRevoked(true);
            token.setUpdatedAt(now);
            token.setDeletedAt(now);
            tokenRepository.save(token);

        } else {
            throw  new CustomException("Liên kết xác thực hết hạn hoặc không hợp lệ", HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public boolean changePassword(String tokenStr, ChangePasswordRequest changePasswordRequest) {
        Token token = tokenRepository.findByAccessToken(tokenStr)
                .orElseThrow(() -> new CustomException("Không tìm thấy token", HttpStatus.NOT_FOUND));
        String username = jwtTokenUtil.getUsernameFromToken(token.getAccessToken());
        User user = userRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new CustomException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

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
                .filter(user -> user.getDeletedAt() == null)
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
                .orElseThrow(() -> new CustomException("Không tìm thấy token", HttpStatus.NOT_FOUND));

        User user = userRepository.findById(token.getUser().getUserId())
                .orElseThrow(() -> new CustomException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        Customer customer = customerRepository.findByUserAndDeletedAtIsNull(token.getUser())
                .orElseThrow(() -> new CustomException("Không tìm thấy khách hàng", HttpStatus.NOT_FOUND));

        return mapCustomerToUserCustomerResponse(customer);
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
    public CreateStudentResponse createStudentByParent(String jwtToken, CreateStudentRequest studentRequest) {
        Token token = tokenRepository.findByAccessToken(jwtToken)
                .orElseThrow(() -> new CustomException("Không tìm thấy token", HttpStatus.NOT_FOUND));

        Parent parent = parentRepository.findByUserAndDeletedAtIsNull(token.getUser())
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
                .email(parent.getUser().getEmail())
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
                .phone(parent.getPhone())
                .address("")
                .user(savedUser)
                .grade(studentRequest.getGrade())
                .parent(parent)
                .className("")
                .school("")
                .build();

        Student savedStudent = studentRepository.save(student);

        parent.getStudents().add(savedStudent);
        parentRepository.save(parent);

        return mapData.mapOne(savedUser, CreateStudentResponse.class);
    }

    @Override
    public UserCustomerResponse changeUserInfo(String jwtToken, UserRequest request) {
        Token token = tokenRepository.findByAccessToken(jwtToken)
                .orElseThrow(() -> new CustomException("Không tìm thấy token", HttpStatus.NOT_FOUND));

        User user = userRepository.findById(token.getUser().getUserId())
                .orElseThrow(() -> new CustomException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        Customer customer = customerRepository.findByUserAndDeletedAtIsNull(token.getUser())
                .orElseThrow(() -> new CustomException("Không tìm thấy khách hàng", HttpStatus.NOT_FOUND));

        customer.setFullName(request.getFullName());
        customer.setDistrict(request.getDistrict());
        customer.setCity(request.getCity());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setAddress(request.getAddress());

        if (customer instanceof Student student) {
            student.setClassName(request.getClassName());
            student.setSchool(request.getSchool());
        }

        userRepository.save(user);
        Customer savedCustomer = customerRepository.save(customer);

        return mapCustomerToUserCustomerResponse(savedCustomer);
    }

    @Override
    public List<UserCustomerResponse> getListStudentByParentUser(String jwtToken) {
        Token token = tokenRepository.findByAccessToken(jwtToken)
                .orElseThrow(() -> new CustomException("Không tìm thấy token", HttpStatus.NOT_FOUND));

        Parent parent = parentRepository.findByUserAndDeletedAtIsNull(token.getUser())
                .orElseThrow(() -> new CustomException("Không tìm thấy tài khoản phụ huynh", HttpStatus.NOT_FOUND));

        return parent.getStudents().stream()
                .map(this::mapCustomerToUserCustomerResponse)
                .collect(Collectors.toList());
    }

    private UserCustomerResponse mapCustomerToUserCustomerResponse(Customer customer) {
        User user = customer.getUser();
        UserCustomerResponse.UserCustomerResponseBuilder responseBuilder = UserCustomerResponse.builder()
                .userId(user.getUserId())
                .fullName(customer.getFullName())
                .username(user.getUsername())
                .role(user.getRole().name())
                .district(customer.getDistrict())
                .city(customer.getCity())
                .dateOfBirth(customer.getDateOfBirth())
                .phone(customer.getPhone())
                .email(user.getEmail())
                .address(customer.getAddress());

        if (customer instanceof Student student) {
            responseBuilder.grade(student.getGrade())
                    .className(student.getClassName())
                    .school(student.getSchool());
        }

        return responseBuilder.build();
    }
}