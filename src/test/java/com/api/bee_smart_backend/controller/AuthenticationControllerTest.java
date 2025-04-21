package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.CreateUserRequest;
import com.api.bee_smart_backend.helper.response.CreateUserResponse;
import com.api.bee_smart_backend.helper.response.ResponseObject;
import com.api.bee_smart_backend.repository.UserRepository;
import com.api.bee_smart_backend.repository.TokenRepository;
import com.api.bee_smart_backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AuthenticationControllerTest {

    @InjectMocks
    private AuthenticationController authenticationController;

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenRepository tokenRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRegisterUser_Success() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setEmail("testuser@example.com");
        request.setPassword("password123");
        request.setRole("PARENT");

        CreateUserResponse mockResponse = new CreateUserResponse();
        mockResponse.setUsername("testuser");
        mockResponse.setEmail("testuser@example.com");
        mockResponse.setToken("mockToken");

        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(mockResponse);

        // Act
        ResponseEntity<ResponseObject<CreateUserResponse>> response = authenticationController.register(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Đăng ký thành công. Vui lòng kiểm tra email để xác thực tài khoản", response.getBody().getMessage());

        CreateUserResponse userResponse = (CreateUserResponse) response.getBody().getData();
        assertNotNull(userResponse);
        assertEquals("testuser", userResponse.getUsername());


        verify(userService, times(1)).createUser(any(CreateUserRequest.class));
    }

    @Test
    void testRegisterUser_UsernameExists() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("existinguser");
        request.setEmail("existinguser@example.com");
        request.setPassword("password123");
        request.setRole("PARENT");

        when(userService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new CustomException("Username đã tồn tại", HttpStatus.CONFLICT));

        // Act
        ResponseEntity<ResponseObject<CreateUserResponse>> response = authenticationController.register(request);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Username đã tồn tại", response.getBody().getMessage());

        verify(userService, times(1)).createUser(any(CreateUserRequest.class));
    }

    @Test
    void testRegisterUser_EmailExists() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setEmail("existingemail@example.com");
        request.setPassword("password123");
        request.setRole("PARENT");

        when(userService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new CustomException("Email đã tồn tại", HttpStatus.CONFLICT));

        // Act
        ResponseEntity<ResponseObject<CreateUserResponse>> response = authenticationController.register(request);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Email đã tồn tại", response.getBody().getMessage());

        verify(userService, times(1)).createUser(any(CreateUserRequest.class));
    }

    @Test
    void testRegisterUser_InternalServerError() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setEmail("testuser@example.com");
        request.setPassword("password123");
        request.setRole("PARENT");

        when(userService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act
        ResponseEntity<ResponseObject<CreateUserResponse>> response = authenticationController.register(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("Lỗi đăng ký"));

        verify(userService, times(1)).createUser(any(CreateUserRequest.class));
    }
}
