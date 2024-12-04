package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.CreateStudentRequest;
import com.api.bee_smart_backend.helper.request.CreateUserRequest;
import com.api.bee_smart_backend.helper.response.CreateStudentResponse;
import com.api.bee_smart_backend.helper.response.CreateUserResponse;
import com.api.bee_smart_backend.helper.response.UserCustomerResponse;
import com.api.bee_smart_backend.helper.response.UserResponse;

import java.util.List;

public interface UserService {
    CreateUserResponse createUser(CreateUserRequest userRequest);

    String verifyEmail(String token);

    List<UserResponse> getAllUsers();

    UserCustomerResponse getUserInfo(String jwtToken);

    void deleteUsersByIds(List<String> userIds);

    void deactivateUser(String userId, boolean activeStatus);

    CreateStudentResponse createStudentByParent(String parentId, CreateStudentRequest studentRequest);
}
