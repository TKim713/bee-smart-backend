package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.config.JwtTokenUtil;
import com.api.bee_smart_backend.config.MapData;
import com.api.bee_smart_backend.helper.enums.Role;
import com.api.bee_smart_backend.helper.enums.TokenType;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.ChangePasswordRequest;
import com.api.bee_smart_backend.helper.request.CreateStudentRequest;
import com.api.bee_smart_backend.helper.request.CreateUserRequest;
import com.api.bee_smart_backend.helper.request.UserRequest;
import com.api.bee_smart_backend.helper.response.CreateStudentResponse;
import com.api.bee_smart_backend.helper.response.CreateUserResponse;
import com.api.bee_smart_backend.helper.response.UserCustomerResponse;
import com.api.bee_smart_backend.helper.response.UserResponse;
import com.api.bee_smart_backend.model.*;
import com.api.bee_smart_backend.repository.*;
import com.api.bee_smart_backend.service.EmailService;
import com.api.bee_smart_backend.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private ParentRepository parentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private MapData mapData;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private Parent testParent;
    private Student testStudent;
    private Token testToken;
    private CreateUserRequest createUserRequest;
    private CreateStudentRequest createStudentRequest;
    private Instant now;

    @BeforeEach
    void setUp() {
        // Initialize mocks
        // MockitoAnnotations.openMocks(this); // Not needed with @ExtendWith(MockitoExtension.class)

        // Set @Autowired fields explicitly
        ReflectionTestUtils.setField(userService, "jwtTokenUtil", jwtTokenUtil);
        ReflectionTestUtils.setField(userService, "passwordEncoder", passwordEncoder);

        now = Instant.now();

        testUser = User.builder()
                .userId("user1")
                .username("testUser")
                .email("test@example.com")
                .password("encodedPassword")
                .role(Role.PARENT)
                .enabled(false)
                .active(true)
                .createdAt(now)
                .build();

        testParent = Parent.builder()
                .customerId("customer1")
                .fullName("Test Parent")
                .user(testUser)
                .students(new ArrayList<>())
                .createdAt(now)
                .build();

        testStudent = Student.builder()
                .customerId("customer2")
                .fullName("Test Student")
                .user(testUser)
                .grade("Lớp 1")
                .parent(testParent)
                .className("")
                .school("")
                .createdAt(now)
                .build();

        testToken = Token.builder()
                .accessToken("testToken")
                .tokenType(TokenType.VERIFY)
                .user(testUser)
                .expirationTime(now.plus(Duration.ofMinutes(10)))
                .createdAt(now)
                .expired(false)
                .revoked(false)
                .build();

        createUserRequest = CreateUserRequest.builder()
                .username("testUser")
                .email("test@example.com")
                .password("password")
                .role("PARENT")
                .fullName("Test Parent")
                .build();

        createStudentRequest = CreateStudentRequest.builder()
                .username("studentUser")
                .password("password")
                .role("STUDENT")
                .fullName("Test Student")
                .grade("Lớp 1")
                .build();
    }

    @Test
    void createUser_Parent_Success() {
        // Arrange
        CreateUserResponse mappedResponse = CreateUserResponse.builder()
                .username("testUser")
                .email("test@example.com")
                .role("PARENT")
                .build();

        when(userRepository.findByUsernameAndDeletedAtIsNull("testUser")).thenReturn(Optional.empty());
        when(userRepository.findByEmailAndDeletedAtIsNull("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(customerRepository.save(any(Parent.class))).thenReturn(testParent);
        when(tokenRepository.save(any(Token.class))).thenReturn(testToken);
        when(mapData.mapOne(testUser, CreateUserResponse.class)).thenReturn(mappedResponse);

        // Act
        CreateUserResponse response = userService.createUser(createUserRequest);

        // Assert
        assertNotNull(response);
        assertEquals("testUser", response.getUsername());
        assertEquals("testToken", response.getToken());

        verify(userRepository).findByUsernameAndDeletedAtIsNull("testUser");
        verify(userRepository).findByEmailAndDeletedAtIsNull("test@example.com");
        verify(passwordEncoder).encode("password");
        verify(userRepository).save(any(User.class));
        verify(customerRepository).save(any(Parent.class));
        verify(tokenRepository).save(any(Token.class));
        verify(emailService).sendEmailWithTemplate(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void createUser_Student_Success() {
        // Arrange
        createUserRequest.setRole("STUDENT");
        createUserRequest.setGrade("Lớp 1");
        createUserRequest.setSchool("Test School");

        CreateUserResponse mappedResponse = CreateUserResponse.builder()
                .username("testUser")
                .email("test@example.com")
                .role("STUDENT")
                .build();

        when(userRepository.findByUsernameAndDeletedAtIsNull("testUser")).thenReturn(Optional.empty());
        when(userRepository.findByEmailAndDeletedAtIsNull("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(customerRepository.save(any(Student.class))).thenReturn(testStudent);
        when(tokenRepository.save(any(Token.class))).thenReturn(testToken);
        when(mapData.mapOne(testUser, CreateUserResponse.class)).thenReturn(mappedResponse);

        // Act
        CreateUserResponse response = userService.createUser(createUserRequest);

        // Assert
        assertNotNull(response);
        assertEquals("testUser", response.getUsername());
        assertEquals("testToken", response.getToken());

        verify(userRepository).findByUsernameAndDeletedAtIsNull("testUser");
        verify(userRepository).findByEmailAndDeletedAtIsNull("test@example.com");
        verify(passwordEncoder).encode("password");
        verify(userRepository).save(any(User.class));
        verify(customerRepository).save(any(Student.class));
        verify(tokenRepository).save(any(Token.class));
        verify(emailService).sendEmailWithTemplate(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void createUser_UsernameExists_ThrowsException() {
        // Arrange
        when(userRepository.findByUsernameAndDeletedAtIsNull("testUser")).thenReturn(Optional.of(testUser));

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> userService.createUser(createUserRequest));
        assertEquals("Username đã tồn tại", exception.getMessage());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());

        verify(userRepository).findByUsernameAndDeletedAtIsNull("testUser");
    }

    @Test
    void createUser_EmailExists_ThrowsException() {
        // Arrange
        when(userRepository.findByUsernameAndDeletedAtIsNull("testUser")).thenReturn(Optional.empty());
        when(userRepository.findByEmailAndDeletedAtIsNull("test@example.com")).thenReturn(Optional.of(testUser));

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> userService.createUser(createUserRequest));
        assertEquals("Email đã tồn tại", exception.getMessage());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());

        verify(userRepository).findByUsernameAndDeletedAtIsNull("testUser");
        verify(userRepository).findByEmailAndDeletedAtIsNull("test@example.com");
    }

    @Test
    void verifyEmail_Success() {
        // Arrange
        when(tokenRepository.findByAccessToken("testToken")).thenReturn(Optional.of(testToken));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(tokenRepository.save(any(Token.class))).thenReturn(testToken);

        // Act
        userService.verifyEmail("testToken");

        // Assert
        verify(tokenRepository).findByAccessToken("testToken");
        verify(userRepository).save(argThat(user -> user.isEnabled()));
        verify(tokenRepository).save(argThat(token -> token.isExpired() && token.isRevoked()));
    }

    @Test
    void verifyEmail_TokenExpired_ThrowsException() {
        // Arrange
        testToken.setExpirationTime(now.minus(Duration.ofMinutes(1)));
        when(tokenRepository.findByAccessToken("testToken")).thenReturn(Optional.of(testToken));

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> userService.verifyEmail("testToken"));
        assertEquals("Liên kết xác thực hết hạn hoặc không hợp lệ", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(tokenRepository).findByAccessToken("testToken");
    }

    @Test
    void changePassword_Success() {
        // Arrange
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("oldPassword")
                .newPassword("newPassword")
                .build();

        when(tokenRepository.findByAccessToken("testToken")).thenReturn(Optional.of(testToken));
        when(jwtTokenUtil.getUsernameFromToken("testToken")).thenReturn("testUser");
        when(userRepository.findByUsernameAndDeletedAtIsNull("testUser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        boolean result = userService.changePassword("testToken", request);

        // Assert
        assertTrue(result);
        verify(tokenRepository).findByAccessToken("testToken");
        verify(jwtTokenUtil).getUsernameFromToken("testToken");
        verify(userRepository).findByUsernameAndDeletedAtIsNull("testUser");
        verify(passwordEncoder).matches("oldPassword", "encodedPassword");
        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void changePassword_SameOldAndNewPassword_ThrowsException() {
        // Arrange
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("password")
                .newPassword("password")
                .build();

        when(tokenRepository.findByAccessToken("testToken")).thenReturn(Optional.of(testToken));
        when(jwtTokenUtil.getUsernameFromToken("testToken")).thenReturn("testUser");
        when(userRepository.findByUsernameAndDeletedAtIsNull("testUser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () ->
                userService.changePassword("testToken", request));
        assertEquals("Mật khẩu mới không được trùng với mật khẩu cũ", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());

        verify(tokenRepository).findByAccessToken("testToken");
        verify(jwtTokenUtil).getUsernameFromToken("testToken");
        verify(userRepository).findByUsernameAndDeletedAtIsNull("testUser");
        verify(passwordEncoder).matches("password", "encodedPassword");
    }

    @Test
    void getAllUsers_Success() {
        // Arrange
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        // Act
        List<UserResponse> result = userService.getAllUsers();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testUser", result.get(0).getUsername());
        verify(userRepository).findAll();
    }

    @Test
    void getUserInfo_Parent_Success() {
        // Arrange
        when(tokenRepository.findByAccessToken("testToken")).thenReturn(Optional.of(testToken));
        when(userRepository.findById("user1")).thenReturn(Optional.of(testUser));
        when(customerRepository.findByUserAndDeletedAtIsNull(testUser)).thenReturn(Optional.of(testParent));

        // Act
        UserCustomerResponse result = userService.getUserInfo("testToken");

        // Assert
        assertNotNull(result);
        assertEquals("testUser", result.getUsername());
        assertEquals("Test Parent", result.getFullName());

        verify(tokenRepository).findByAccessToken("testToken");
        verify(userRepository).findById("user1");
        verify(customerRepository).findByUserAndDeletedAtIsNull(testUser);
    }

    @Test
    void getUserInfo_CustomerNotFound_ThrowsException() {
        // Arrange
        when(tokenRepository.findByAccessToken("testToken")).thenReturn(Optional.of(testToken));
        when(userRepository.findById("user1")).thenReturn(Optional.of(testUser));
        when(customerRepository.findByUserAndDeletedAtIsNull(testUser)).thenReturn(Optional.empty());

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> userService.getUserInfo("testToken"));
        assertEquals("Không tìm thấy khách hàng", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(tokenRepository).findByAccessToken("testToken");
        verify(userRepository).findById("user1");
        verify(customerRepository).findByUserAndDeletedAtIsNull(testUser);
    }

    @Test
    void deleteUsersByIds_Success() {
        // Arrange
        List<String> userIds = List.of("user1");
        when(userRepository.findById("user1")).thenReturn(Optional.of(testUser));
        when(customerRepository.findByUserAndDeletedAtIsNull(testUser)).thenReturn(Optional.of(testParent));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(customerRepository.save(any(Customer.class))).thenReturn(testParent);

        // Act
        userService.deleteUsersByIds(userIds);

        // Assert
        verify(userRepository).findById("user1");
        verify(customerRepository).findByUserAndDeletedAtIsNull(testUser);
        verify(userRepository).save(any(User.class));
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void deactivateUser_Success() {
        // Arrange
        String userId = "user1";
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.deactivateUser(userId, false);

        // Assert
        assertFalse(testUser.isActive());
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void deactivateUser_AlreadyDeactivated_ThrowsException() {
        // Arrange
        String userId = "user1";
        testUser.setActive(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () -> userService.deactivateUser(userId, false));
        assertEquals("Người dùng đã được vô hiệu hóa trước đó", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());

        verify(userRepository).findById(userId);
    }

    @Test
    void createStudentByParent_Success() {
        // Arrange
        User studentUser = User.builder()
                .userId("student1")
                .username("studentUser")
                .email("test@example.com")
                .password("encodedPassword")
                .role(Role.STUDENT)
                .enabled(true)
                .active(true)
                .createdAt(now)
                .build();

        Student newStudent = Student.builder()
                .customerId("customer3")
                .fullName("Test Student")
                .user(studentUser)
                .grade("Lớp 1")
                .parent(testParent)
                .className("")
                .school("")
                .createdAt(now)
                .build();

        CreateStudentResponse mappedResponse = CreateStudentResponse.builder()
                .username("studentUser")
                .role("STUDENT")
                .build();

        when(tokenRepository.findByAccessToken("testToken")).thenReturn(Optional.of(testToken));
        when(parentRepository.findByUserAndDeletedAtIsNull(testUser)).thenReturn(Optional.of(testParent));
        when(userRepository.findByUsernameAndDeletedAtIsNull("studentUser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(studentUser);
        when(studentRepository.save(any(Student.class))).thenReturn(newStudent);
        when(parentRepository.save(any(Parent.class))).thenReturn(testParent);
        when(mapData.mapOne(studentUser, CreateStudentResponse.class)).thenReturn(mappedResponse);

        // Act
        CreateStudentResponse result = userService.createStudentByParent("testToken", createStudentRequest);

        // Assert
        assertNotNull(result);
        assertEquals("studentUser", result.getUsername());
        assertEquals("STUDENT", result.getRole());

        verify(tokenRepository).findByAccessToken("testToken");
        verify(parentRepository).findByUserAndDeletedAtIsNull(testUser);
        verify(userRepository).findByUsernameAndDeletedAtIsNull("studentUser");
        verify(passwordEncoder).encode("password");
        verify(userRepository).save(any(User.class));
        verify(parentRepository).save(any(Parent.class));
    }

    @Test
    void createStudentByParent_NullGrade_ThrowsException() {
        // Arrange
        createStudentRequest.setGrade(null);
        when(tokenRepository.findByAccessToken("testToken")).thenReturn(Optional.of(testToken));
        when(parentRepository.findByUserAndDeletedAtIsNull(testUser)).thenReturn(Optional.of(testParent));

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () ->
                userService.createStudentByParent("testToken", createStudentRequest));
        assertEquals("Grade is required when creating a student account", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());

        verify(tokenRepository).findByAccessToken("testToken");
        verify(parentRepository).findByUserAndDeletedAtIsNull(testUser);
    }

    @Test
    void changeUserInfo_Student_Success() {
        // Arrange
        UserRequest request = UserRequest.builder()
                .fullName("Updated Name")
                .district("District")
                .city("City")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .address("")
                .className("Class 1")
                .school("School")
                .build();

        when(tokenRepository.findByAccessToken("testToken")).thenReturn(Optional.of(testToken));
        when(userRepository.findById("user1")).thenReturn(Optional.of(testUser));
        when(customerRepository.findByUserAndDeletedAtIsNull(testUser)).thenReturn(Optional.of(testStudent));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(customerRepository.save(any(Customer.class))).thenReturn(testStudent);

        // Act
        UserCustomerResponse result = userService.changeUserInfo("testToken", request);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Name", result.getFullName());
        assertEquals("Lớp 1", result.getGrade());
        assertEquals("Class 1", result.getClassName());

        verify(tokenRepository).findByAccessToken("testToken");
        verify(userRepository).findById("user1");
        verify(customerRepository).findByUserAndDeletedAtIsNull(testUser);
        verify(userRepository).save(any(User.class));
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void getListStudentByParentUser_Success() {
        // Arrange
        testParent.getStudents().add(testStudent);
        when(tokenRepository.findByAccessToken("testToken")).thenReturn(Optional.of(testToken));
        when(parentRepository.findByUserAndDeletedAtIsNull(testUser)).thenReturn(Optional.of(testParent));

        // Act
        List<UserCustomerResponse> result = userService.getListStudentByParentUser("testToken");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Student", result.get(0).getFullName());
        assertEquals("Lớp 1", result.get(0).getGrade());

        verify(tokenRepository).findByAccessToken("testToken");
        verify(parentRepository).findByUserAndDeletedAtIsNull(testUser);
    }

    @Test
    void getListStudentByParentUser_ParentNotFound_ThrowsException() {
        // Arrange
        when(tokenRepository.findByAccessToken("testToken")).thenReturn(Optional.of(testToken));
        when(parentRepository.findByUserAndDeletedAtIsNull(testUser)).thenReturn(Optional.empty());

        // Act & Assert
        CustomException exception = assertThrows(CustomException.class, () ->
                userService.getListStudentByParentUser("testToken"));
        assertEquals("Không tìm thấy tài khoản phụ huynh", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());

        verify(tokenRepository).findByAccessToken("testToken");
        verify(parentRepository).findByUserAndDeletedAtIsNull(testUser);
    }
}