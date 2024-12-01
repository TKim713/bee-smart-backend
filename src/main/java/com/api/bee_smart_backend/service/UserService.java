package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.CreateUserRequest;
import com.api.bee_smart_backend.helper.response.CreateUserResponse;
import com.api.bee_smart_backend.helper.response.UserResponse;

import java.util.List;

public interface UserService {
    CreateUserResponse createUser(CreateUserRequest userRequest);

    String verifyEmail(String token);

    List<UserResponse> getAllUsers();

    void deleteUserById(String userId);

    void deactivateUser(String userId, boolean activeStatus);
}
