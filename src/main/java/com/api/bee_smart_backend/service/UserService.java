package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.ChangePasswordRequest;
import com.api.bee_smart_backend.helper.request.CreateStudentRequest;
import com.api.bee_smart_backend.helper.request.CreateUserRequest;
import com.api.bee_smart_backend.helper.request.UserRequest;
import com.api.bee_smart_backend.helper.response.CreateStudentResponse;
import com.api.bee_smart_backend.helper.response.CreateUserResponse;
import com.api.bee_smart_backend.helper.response.UserCustomerResponse;
import com.api.bee_smart_backend.helper.response.UserResponse;
import com.api.bee_smart_backend.model.Student;

import java.util.List;

public interface UserService {
    CreateUserResponse createUser(CreateUserRequest userRequest);

    void verifyEmail(String token);

    boolean changePassword(String tokenStr, ChangePasswordRequest changePasswordRequest);

    List<UserResponse> getAllUsers();

    UserCustomerResponse getUserInfo(String jwtToken);

    void deleteUsersByIds(List<String> userIds);

    void deactivateUser(String userId, boolean activeStatus);

    CreateStudentResponse createStudentByParent(String jwtToken, CreateStudentRequest studentRequest);

    UserCustomerResponse changeUserInfo(String jwtToken, UserRequest request);

    List<UserCustomerResponse> getListStudentByParentUser(String jwtToken);
}
