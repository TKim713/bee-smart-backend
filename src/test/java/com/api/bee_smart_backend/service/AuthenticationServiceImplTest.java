package com.api.bee_smart_backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import com.api.bee_smart_backend.config.JwtTokenUtil;
import com.api.bee_smart_backend.config.JwtUserDetailsService;
import com.api.bee_smart_backend.helper.enums.Role;
import com.api.bee_smart_backend.helper.enums.TokenType;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.JwtRequest;
import com.api.bee_smart_backend.helper.request.ResetPasswordRequest;
import com.api.bee_smart_backend.helper.request.VerifyOtpRequest;
import com.api.bee_smart_backend.helper.response.JwtResponse;
import com.api.bee_smart_backend.model.Student;
import com.api.bee_smart_backend.model.Token;
import com.api.bee_smart_backend.model.User;
import com.api.bee_smart_backend.repository.StudentRepository;
import com.api.bee_smart_backend.repository.TokenRepository;
import com.api.bee_smart_backend.repository.UserRepository;
import com.api.bee_smart_backend.service.impl.AuthenticationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class AuthenticationServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private JwtUserDetailsService jwtUserDetailsService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    private User user;
    private Student student;
    private Token token;
    private JwtRequest jwtRequest;
    private UserDetails userDetails;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        user = User.builder()
                .userId(String.valueOf(1L))
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .role(Role.STUDENT)
                .enabled(true)
                .active(true)
                .isOnline(false)
                .build();

        student = Student.builder()
                .user(user)
                .grade("10")
                .build();

        token = Token.builder()
                .accessToken("accessToken")
                .refreshToken("refreshToken")
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .user(user)
                .createdAt(Instant.now())
                .build();

        jwtRequest = new JwtRequest("testuser", "password");

        userDetails = new org.springframework.security.core.userdetails.User(
                "testuser", "encodedPassword", new ArrayList<>());
    }

    @Test
    public void testAuthenticate_Success() {
        when(userRepository.findByUsernameAndDeletedAtIsNull("testuser")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("testuser", "password"));
        when(jwtUserDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtTokenUtil.generateToken(userDetails)).thenReturn("accessToken");
        when(jwtTokenUtil.generateRefreshToken(userDetails)).thenReturn("refreshToken");
        when(studentRepository.findByUserAndDeletedAtIsNull(user)).thenReturn(Optional.of(student));
        when(tokenRepository.save(any(Token.class))).thenReturn(token);
        when(userRepository.save(any(User.class))).thenReturn(user);

        JwtResponse response = authenticationService.authenticate(jwtRequest);

        assertNotNull(response);
        assertEquals("accessToken", response.getAccessToken());
        assertEquals("refreshToken", response.getRefreshToken());
        assertEquals(user.getUserId(), response.getUserId());
        assertEquals(user.getUsername(), response.getUsername());
        assertEquals("STUDENT", response.getRole());
        assertEquals("10", response.getGrade());

        // Capture the Token object passed to tokenRepository.save
        ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        Token savedToken = tokenCaptor.getValue();
        assertEquals("accessToken", savedToken.getAccessToken());
        assertEquals("refreshToken", savedToken.getRefreshToken());
        assertEquals(TokenType.BEARER, savedToken.getTokenType());
        assertFalse(savedToken.isExpired(), "Token should not be expired");
        assertFalse(savedToken.isRevoked(), "Token should not be revoked");

        // Capture the User object passed to userRepository.save
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertTrue(savedUser.isOnline(), "User should be marked as online");

        verify(userRepository).findByUsernameAndDeletedAtIsNull("testuser");
        verify(studentRepository).findByUserAndDeletedAtIsNull(user);
    }

    @Test
    public void testAuthenticate_UserNotFound() {
        when(userRepository.findByUsernameAndDeletedAtIsNull("testuser")).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class, () -> {
            authenticationService.authenticate(jwtRequest);
        });

        assertEquals("Không tìm thấy tài khoản", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    public void testAuthenticate_BadCredentials() {
        when(userRepository.findByUsernameAndDeletedAtIsNull("testuser")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        CustomException exception = assertThrows(CustomException.class, () -> {
            authenticationService.authenticate(jwtRequest);
        });

        assertEquals("Mật khẩu không đúng", exception.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    public void testLogout_Success() {
        when(tokenRepository.findByAccessToken("accessToken")).thenReturn(Optional.of(token));
        when(userRepository.findByUserIdAndDeletedAtIsNull(user.getUserId())).thenReturn(Optional.of(user));
        when(tokenRepository.save(any(Token.class))).thenReturn(token);
        when(userRepository.save(any(User.class))).thenReturn(user);

        authenticationService.logout("accessToken");

        // Capture the Token object passed to tokenRepository.save
        ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        Token savedToken = tokenCaptor.getValue();

        // Verify the state of the saved token
        assertTrue(savedToken.isExpired(), "Token should be marked as expired");
        assertTrue(savedToken.isRevoked(), "Token should be marked as revoked");
        assertNotNull(savedToken.getUpdatedAt(), "Token updatedAt should be set");
        assertNotNull(savedToken.getDeletedAt(), "Token deletedAt should be set");

        // Capture the User object passed to userRepository.save
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        // Verify the state of the saved user
        assertFalse(savedUser.isOnline(), "User should be marked as offline");

        // Verify that the repository methods were called
        verify(tokenRepository).findByAccessToken("accessToken");
        verify(userRepository).findByUserIdAndDeletedAtIsNull(user.getUserId());
    }

    @Test
    public void testLogout_TokenNotFound() {
        when(tokenRepository.findByAccessToken("accessToken")).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class, () -> {
            authenticationService.logout("accessToken");
        });

        assertEquals("Không tìm thấy token", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    public void testResendConfirmationEmail_Success() {
        user.setEnabled(false);
        when(userRepository.findByEmailAndDeletedAtIsNull("test@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findFirstByUserAndTokenTypeOrderByUpdatedAtDescCreatedAtDesc(user, TokenType.VERIFY))
                .thenReturn(Optional.empty());
        when(tokenRepository.save(any(Token.class))).thenReturn(token);

        String result = authenticationService
                .resendConfirmationEmail("test@example.com");

        assertEquals("Một email xác nhận mới đã được gửi tới test@example.com", result);
        verify(emailService).sendEmailWithTemplate(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(tokenRepository).save(any(Token.class));
    }

    @Test
    public void testResendConfirmationEmail_AlreadyEnabled() {
        when(userRepository.findByEmailAndDeletedAtIsNull("test@example.com")).thenReturn(Optional.of(user));

        CustomException exception = assertThrows(CustomException.class, () -> {
            authenticationService.resendConfirmationEmail("test@example.com");
        });

        assertEquals("Tài khoản đã được xác thực", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    public void testRefreshToken_Success() {
        when(jwtTokenUtil.validateRefreshToken("refreshToken")).thenReturn(true);
        when(tokenRepository.findByRefreshToken("refreshToken")).thenReturn(token);
        when(jwtUserDetailsService.loadUserByUsername(user.getUsername())).thenReturn(userDetails);
        when(jwtTokenUtil.generateToken(userDetails)).thenReturn("newAccessToken");
        when(tokenRepository.save(any(Token.class))).thenReturn(token);

        JwtResponse response = authenticationService.refreshToken("refreshToken");

        assertNotNull(response);
        assertEquals("newAccessToken", response.getAccessToken());
        assertEquals("refreshToken", response.getRefreshToken());
        verify(tokenRepository).save(token);
    }

    @Test
    public void testRefreshToken_InvalidToken() {
        when(jwtTokenUtil.validateRefreshToken("refreshToken")).thenReturn(false);
        when(tokenRepository.findByRefreshToken("refreshToken")).thenReturn(token);
        when(tokenRepository.save(any(Token.class))).thenReturn(token);

        CustomException exception = assertThrows(CustomException.class, () -> {
            authenticationService.refreshToken("refreshToken");
        });

        assertEquals("Refresh token không hợp lệ hoặc đã hết hạn", exception.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());

        // Capture the Token object passed to tokenRepository.save
        ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        Token savedToken = tokenCaptor.getValue();
        assertTrue(savedToken.isExpired(), "Token should be marked as expired");
        assertTrue(savedToken.isRevoked(), "Token should be marked as revoked");

        verify(jwtTokenUtil).validateRefreshToken("refreshToken");
        verify(tokenRepository).findByRefreshToken("refreshToken");
    }

    @Test
    public void testForgotPassword_Success() {
        when(userRepository.findByEmailAndDeletedAtIsNull("test@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.save(any(Token.class))).thenReturn(token);

        boolean result = authenticationService.forgotPassword("test@example.com");

        assertTrue(result);
        verify(emailService).sendEmailWithTemplate(anyString(), anyString(), anyString(), anyString(), anyInt());
        verify(tokenRepository).save(any(Token.class));
    }

    @Test
    public void testVerifyOtp_Success() {
        when(userRepository.findByEmailAndDeletedAtIsNull("test@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findByAccessTokenAndUser("123456", user)).thenReturn(Optional.of(token));
        when(tokenRepository.save(any(Token.class))).thenReturn(token);

        VerifyOtpRequest request = new VerifyOtpRequest("test@example.com", "123456");
        String result = authenticationService.verifyOtp(request);

        assertNotNull(result);
        verify(tokenRepository).save(any(Token.class));
    }

    @Test
    public void testVerifyOtp_InvalidOtp() {
        when(userRepository.findByEmailAndDeletedAtIsNull("test@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findByAccessTokenAndUser("123456", user)).thenReturn(Optional.empty());

        VerifyOtpRequest request = new VerifyOtpRequest("test@example.com", "123456");

        CustomException exception = assertThrows(CustomException.class, () -> {
            authenticationService.verifyOtp(request);
        });

        assertEquals("OTP không hợp lệ", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    public void testResetPassword_Success() {
        when(tokenRepository.findByAccessToken("tempToken")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(tokenRepository.save(any(Token.class))).thenReturn(token);

        ResetPasswordRequest request = new ResetPasswordRequest("tempToken", "newPassword");
        boolean result = authenticationService.resetPassword(request);

        assertTrue(result);

        // Capture the User object passed to userRepository.save
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("encodedNewPassword", savedUser.getPassword(), "User password should be updated");

        // Capture the Token object passed to tokenRepository.save
        ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        Token savedToken = tokenCaptor.getValue();
        assertTrue(savedToken.isExpired(), "Token should be marked as expired");
        assertTrue(savedToken.isRevoked(), "Token should be marked as revoked");
        assertNotNull(savedToken.getUpdatedAt(), "Token updatedAt should be set");

        verify(tokenRepository).findByAccessToken("tempToken");
        verify(passwordEncoder).encode("newPassword");
    }

    @Test
    public void testResetPassword_InvalidToken() {
        when(tokenRepository.findByAccessToken("tempToken")).thenReturn(Optional.empty());

        ResetPasswordRequest request = new ResetPasswordRequest("tempToken", "newPassword");

        CustomException exception = assertThrows(CustomException.class, () -> {
            authenticationService.resetPassword(request);
        });

        assertEquals("Token không hợp lệ", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }
}