package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.ChangePasswordRequest;
import com.api.bee_smart_backend.helper.request.CreateStudentRequest;
import com.api.bee_smart_backend.helper.response.CreateStudentResponse;
import com.api.bee_smart_backend.helper.response.ResponseObject;
import com.api.bee_smart_backend.helper.response.UserCustomerResponse;
import com.api.bee_smart_backend.helper.response.UserResponse;
import com.api.bee_smart_backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<ResponseObject<List<UserResponse>>> getListUser() {
        try {
            List<UserResponse> users = userService.getAllUsers();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Danh sách người dùng", users));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage() != null ? e.getMessage() : "Lỗi khi lấy danh sách người dùng", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi bất ngờ xảy ra: " + e.getMessage(), null));
        }
    }

    @GetMapping("/user-info")
    public ResponseEntity<ResponseObject<UserCustomerResponse>> getUserInfo(@RequestHeader("Authorization") String token) {
        String jwtToken = token.replace("Bearer ", "");
        try {
            UserCustomerResponse response = userService.getUserInfo(jwtToken);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Thông tin người dùng", response));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage() != null ? e.getMessage() : "Lỗi khi lấy thông tin người dùng", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi bất ngờ xảy ra: " + e.getMessage(), null));
        }
    }

    @DeleteMapping
    public ResponseEntity<ResponseObject<Object>> deleteUsersByIds(@RequestBody List<String> userIds) {
        try {
            userService.deleteUsersByIds(userIds);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Người dùng và khách hàng đã được xóa thành công", null));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi khi xóa người dùng: " + e.getMessage(), null));
        }
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<ResponseObject<Object>> updateUserStatus(
            @PathVariable String userId,
            @RequestParam boolean active) {
        try {
            userService.deactivateUser(userId, active);
            String message = active ? "Người dùng đã được kích hoạt" : "Người dùng đã bị vô hiệu hóa";
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), message, null));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi khi cập nhật trạng thái người dùng: " + e.getMessage(), null));
        }
    }

    @PostMapping("/parent/{parentId}/student")
    public ResponseEntity<ResponseObject<CreateStudentResponse>> createStudentByParent(
            @PathVariable String parentId,
            @RequestBody @Valid CreateStudentRequest studentRequest) {
        try {
            CreateStudentResponse response = userService.createStudentByParent(parentId, studentRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ResponseObject<>(HttpStatus.CREATED.value(), "Tạo học sinh thành công!", response));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseObject<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Lỗi bất ngờ xảy ra: " + e.getMessage(), null));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<ResponseObject<String>> changePassword(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        String jwtToken = token.replace("Bearer ", "");
        try {
            boolean success = userService.changePassword(jwtToken, changePasswordRequest);
            if (success) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(new ResponseObject<>(HttpStatus.OK.value(), "Đổi mật khẩu thành công", "Success"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Đổi mật khẩu thất bại", null));
            }
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage() != null ? e.getMessage() : "Lỗi đổi mật khẩu", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseObject<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Lỗi bất ngờ: " + e.getMessage(), null));
        }
    }
}
