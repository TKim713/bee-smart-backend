package com.api.bee_smart_backend.service.impl;

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
import com.api.bee_smart_backend.service.AuthenticationService;
import com.api.bee_smart_backend.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private JwtUserDetailsService jwtUserDetailsService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TokenRepository tokenRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private EmailService emailService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    private Instant now = Instant.now();

    @Override
    public JwtResponse authenticate(JwtRequest authenticationRequest) throws CustomException {
        User user = null;
        try {
            user = userRepository.findByUsernameAndDeletedAtIsNull(authenticationRequest.getUsername())
                    .orElseThrow(() -> new CustomException("Không tìm thấy tài khoản", HttpStatus.NOT_FOUND));

            if (!user.isEnabled()) {
                throw new CustomException("Tài khoản chưa xác thực", HttpStatus.FORBIDDEN);
            }

            if (!user.isActive()) {
                throw new CustomException("Tài khoản vô hiệu hóa", HttpStatus.FORBIDDEN);
            }

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authenticationRequest.getUsername(),
                            authenticationRequest.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new CustomException("Mật khẩu không đúng", HttpStatus.UNAUTHORIZED);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(e.getMessage() != null ? e.getMessage() : "Authentication failed", HttpStatus.BAD_REQUEST);
        }

        String grade = null;
        if (user.getRole() == Role.STUDENT) {
            Student student = studentRepository.findByUserAndDeletedAtIsNull(user)
                    .orElseThrow(() -> new CustomException("Không tìm thấy thông tin học sinh", HttpStatus.NOT_FOUND));
            grade = student.getGrade();
        }

        UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(authenticationRequest.getUsername());
        String accessToken = jwtTokenUtil.generateToken(userDetails);
        String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails);

        Token newToken = Token.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .user(user)
                .createdAt(now)
                .build();

        tokenRepository.save(newToken);

        return JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getUserId())
                .username(user.getUsername())
                .role(user.getRole().toString())
                .grade(grade)
                .build();
    }

    @Override
    public void logout(String tokenStr) {
        Token token = tokenRepository.findByAccessToken(tokenStr)
                .orElseThrow(() -> new CustomException("Không tìm thấy token", HttpStatus.NOT_FOUND));
        token.setExpired(true);
        token.setRevoked(true);
        token.setUpdatedAt(now);
        token.setDeletedAt(now);
        tokenRepository.save(token);
    }

    @Override
    public String resendConfirmationEmail(String email) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new CustomException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        if (user.isEnabled()) {
            throw new CustomException("Tài khoản đã được xác thực", HttpStatus.BAD_REQUEST);
        }

        String newToken = UUID.randomUUID().toString();

        Token token = tokenRepository.findByUserAndTokenType(user, TokenType.VERIFY)
                .orElse(new Token());

        token.setAccessToken(newToken);
        token.setExpired(false);
        token.setRevoked(false);
        token.setTokenType(TokenType.VERIFY);
        token.setUser(user);
        token.setCreatedAt(now);
        token.setUpdatedAt(now);

        tokenRepository.save(token);

        emailService.sendEmailWithTemplate(
                user.getEmail(),
                "🌟 Gửi lại email xác thực cho Bee Smart! 🌟",
                EmailServiceImpl.VERIFICATION_EMAIL_TEMPLATE,
                user.getUsername(),
                EmailServiceImpl.BASE_URL + newToken
        );

        return "Một email xác nhận mới đã được gửi tới " + email;
    }

    @Override
    public JwtResponse refreshToken(String refreshToken) {
        if (!jwtTokenUtil.validateRefreshToken(refreshToken)) {
            Token token = tokenRepository.findByRefreshToken(refreshToken);
            if (token != null) {
                token.setExpired(true);
                token.setRevoked(true);
                tokenRepository.save(token);
            }
            throw new CustomException("Refresh token không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED);
        }

        Token token = tokenRepository.findByRefreshToken(refreshToken);
        if (token == null) {
            throw new CustomException("Refresh token không hợp lệ", HttpStatus.UNAUTHORIZED);
        }

        User user = token.getUser();
        UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(user.getUsername());
        String newAccessToken = jwtTokenUtil.generateToken(userDetails);

        token.setAccessToken(newAccessToken);
        token.setUpdatedAt(now);
        tokenRepository.save(token);

        return JwtResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .userId(user.getUserId())
                .username(user.getUsername())
                .role(user.getRole().toString())
                .build();
    }

    @Override
    public boolean forgotPassword(String email) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new CustomException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        int otp = (int) (Math.random() * 900000) + 100000;

        Token token = Token.builder()
                .accessToken(String.valueOf(otp))
                .tokenType(TokenType.OTP)
                .expired(false)
                .revoked(false)
                .user(user)
                .createdAt(now)
                .build();

        tokenRepository.save(token);

        emailService.sendEmailWithTemplate(
                user.getEmail(),
                "🔐 Đặt lại mật khẩu cho Bee Smart!",
                EmailServiceImpl.RESET_PASSWORD_TEMPLATE,
                user.getUsername(),
                otp
        );

        return true;
    }

    @Override
    public String verifyOtp(VerifyOtpRequest verifyOtpRequest) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(verifyOtpRequest.getEmail())
                .orElseThrow(() -> new CustomException("Người dùng không tồn tại", HttpStatus.NOT_FOUND));

        Token token = tokenRepository.findByAccessTokenAndUser(verifyOtpRequest.getOtp(), user)
                .orElseThrow(() -> new CustomException("OTP không hợp lệ", HttpStatus.NOT_FOUND));

        if (token.isExpired() || token.isRevoked()) {
            throw new CustomException("OTP đã hết hạn hoặc không hợp lệ", HttpStatus.BAD_REQUEST);
        }

        String tempToken = UUID.randomUUID().toString();
        token.setAccessToken(tempToken);
        token.setUpdatedAt(now);
        tokenRepository.save(token);

        return tempToken;
    }

    @Override
    public boolean resetPassword(ResetPasswordRequest resetPasswordRequest) {
        Token token = tokenRepository.findByAccessToken(resetPasswordRequest.getToken())
                .orElseThrow(() -> new CustomException("Token không hợp lệ", HttpStatus.NOT_FOUND));

        User user = token.getUser();

        String encodedPassword = passwordEncoder.encode(resetPasswordRequest.getNewPassword());
        user.setPassword(encodedPassword);
        userRepository.save(user);

        token.setExpired(true);
        token.setRevoked(true);
        token.setUpdatedAt(now);
        tokenRepository.save(token);

        return true;
    }
}
