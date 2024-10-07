package com.api.bee_smart_backend.service.impl;

import com.api.bee_smart_backend.helper.enums.Role;
import com.api.bee_smart_backend.helper.request.CreateUserRequest;
import com.api.bee_smart_backend.helper.response.CreateUserResponse;
import com.api.bee_smart_backend.model.User;
import com.api.bee_smart_backend.repository.UserRepository;
import com.api.bee_smart_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private LocalDateTime now = LocalDateTime.now();

    @Override
    public CreateUserResponse createUser(CreateUserRequest userRequest) {

        String password = passwordEncoder.encode(userRequest.getPassword());

        User user = User.builder()
                .username(userRequest.getUsername())
                .email(userRequest.getEmail())
                .password(password)
                .role(Role.valueOf(userRequest.getRole()))
                .create_at(Timestamp.valueOf(now))
                .build();

        User savedUser = userRepository.save(user);

        CreateUserResponse response = new CreateUserResponse();
        response.setUsername(savedUser.getUsername());
        response.setEmail(savedUser.getEmail());
        response.setPassword(user.getPassword());
        response.setRole(user.getRole().toString());
        return response;
    }
}
