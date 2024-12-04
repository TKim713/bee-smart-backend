package com.api.bee_smart_backend.repository;

import com.api.bee_smart_backend.model.Token;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TokenRepository extends MongoRepository<Token, String> {
    Optional<Token> findByAccessToken(String accessToken);
    Token findByRefreshToken(String refreshToken);
}

