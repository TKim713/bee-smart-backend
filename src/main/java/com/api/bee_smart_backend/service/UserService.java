package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.CreateUserRequest;
import com.api.bee_smart_backend.helper.response.CreateUserResponse;

public interface UserService {
    CreateUserResponse createUser(CreateUserRequest userRequest);
}
