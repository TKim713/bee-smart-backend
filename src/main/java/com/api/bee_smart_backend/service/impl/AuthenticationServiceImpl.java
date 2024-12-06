package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.config.JwtTokenUtil;
import com.api.bee_smart_backend.config.JwtUserDetailsService;
import com.api.bee_smart_backend.helper.enums.TokenType;
import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.JwtRequest;
import com.api.bee_smart_backend.helper.response.JwtResponse;
import com.api.bee_smart_backend.model.Token;
import com.api.bee_smart_backend.model.User;
import com.api.bee_smart_backend.repository.TokenRepository;
import com.api.bee_smart_backend.repository.UserRepository;
import com.api.bee_smart_backend.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;

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

    private Instant now = Instant.now();

    @Override
    public JwtResponse authenticate(JwtRequest authenticationRequest) throws CustomException {
        User user = null;
        try {
            user = userRepository.findByUsernameAndDeletedAtIsNull(authenticationRequest.getUsername())
                    .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

            if (!user.isEnabled()) {
                throw new CustomException("ACCOUNT_NOT_VERIFIED", HttpStatus.FORBIDDEN);
            }

            if (!user.isActive()) {
                throw new CustomException("ACCOUNT_NOT_ACTIVE", HttpStatus.FORBIDDEN);
            }

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authenticationRequest.getUsername(),
                            authenticationRequest.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new CustomException("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(e.getMessage() != null ? e.getMessage() : "Authentication failed", HttpStatus.BAD_REQUEST);
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
                .build();
    }

    @Override
    public void logout(String tokenStr) {
        Token token = tokenRepository.findByAccessToken(tokenStr)
                .orElseThrow(() -> new CustomException("Token not found", HttpStatus.NOT_FOUND));
        token.setExpired(true);
        token.setRevoked(true);
        token.setUpdatedAt(now);
        token.setDeletedAt(now);
        tokenRepository.save(token);
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
            throw new CustomException("Invalid or expired refresh token", HttpStatus.UNAUTHORIZED);
        }

        Token token = tokenRepository.findByRefreshToken(refreshToken);
        if (token == null) {
            throw new CustomException("Invalid refresh token", HttpStatus.UNAUTHORIZED);
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
}
