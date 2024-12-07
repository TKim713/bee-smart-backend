package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.helper.enums.TokenType;
import com.api.bee_smart_backend.model.Token;
import com.api.bee_smart_backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TokenRepository extends MongoRepository<Token, String> {
    Optional<Token> findByAccessToken(String accessToken);
    Token findByRefreshToken(String refreshToken);
    Optional<Token> findByUserAndTokenType(User user, TokenType tokenType);

    Optional<Token> findByAccessTokenAndUser(String otp, User user);
}

