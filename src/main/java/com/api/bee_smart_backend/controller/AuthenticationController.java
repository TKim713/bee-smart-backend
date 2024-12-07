package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.CreateUserRequest;
import com.api.bee_smart_backend.helper.request.JwtRequest;
import com.api.bee_smart_backend.helper.request.ResetPasswordRequest;
import com.api.bee_smart_backend.helper.request.VerifyOtpRequest;
import com.api.bee_smart_backend.helper.response.CreateUserResponse;
import com.api.bee_smart_backend.helper.response.JwtResponse;
import com.api.bee_smart_backend.helper.response.ResponseObject;
import com.api.bee_smart_backend.service.AuthenticationService;
import com.api.bee_smart_backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private UserService userService;

    @PostMapping("/authenticate")
    public ResponseEntity<ResponseObject<JwtResponse>> login(@RequestBody JwtRequest authenticationRequest) {
        try {
            JwtResponse jwtResponse = authenticationService.authenticate(authenticationRequest);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Đăng nhập thành công", jwtResponse));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage() != null ? e.getMessage() : "Lỗi khi đăng nhập", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi bất ngờ xảy ra: " + e.getMessage(), null));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ResponseObject<CreateUserResponse>> register(@Valid @RequestBody CreateUserRequest userRequest) {
        try {
            CreateUserResponse userResponse = userService.createUser(userRequest);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Đăng ký thành công. Vui lòng kiểm tra email để xác thực tài khoản", userResponse));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi đăng ký: " + e.getMessage(), null));
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<ResponseObject<Object>> verifyUser(@RequestParam String token) {
        userService.verifyEmail(token);
        try {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Xác thực email thành công!", null));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi đăng ký: " + e.getMessage(), null));
        }
    }

    @PostMapping("/resend-confirm-email")
    public ResponseEntity<ResponseObject<String>> resendConfirmEmail(@RequestParam String email) {
        try {
            String message = authenticationService.resendConfirmationEmail(email);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Gửi lại email xác thực thành công", message)
            );
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseObject<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Lỗi bất ngờ xảy ra", null));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ResponseObject<String>> logout(@RequestHeader("Authorization") String token) {
        String jwtToken = token.replace("Bearer ", "");
        try {
            authenticationService.logout(jwtToken);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Đăng xuất thành công", null));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage() != null ? e.getMessage() : "Lỗi khi đăng xuất", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi bất ngờ xảy ra: " + e.getMessage(), null));
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ResponseObject<JwtResponse>> refreshToken(@RequestBody Map<String, String> refreshTokenRequest) {
        try {
            String refreshToken = refreshTokenRequest.get("refreshToken");
            if (refreshToken == null) {
                throw new CustomException("Thiếu refresh token", HttpStatus.BAD_REQUEST);
            }

            JwtResponse jwtResponse = authenticationService.refreshToken(refreshToken);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Làm mới token thành công", jwtResponse));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage() != null ? e.getMessage() : "Lỗi khi làm mới token", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi bất ngờ: " + e.getMessage(), null));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ResponseObject<String>> forgotPassword(@RequestParam String email) {
        try {
            boolean success = authenticationService.forgotPassword(email);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Mã OTP đã được gửi đến email của bạn", success));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseObject<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Lỗi bất ngờ xảy ra", null));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ResponseObject<String>> verifyOtp(@RequestBody VerifyOtpRequest verifyOtpRequest) {
        try {
            String token = authenticationService.verifyOtp(verifyOtpRequest);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Xác minh OTP thành công", token));
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseObject<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Lỗi bất ngờ xảy ra", null));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ResponseObject<String>> resetPassword(@RequestBody @Valid ResetPasswordRequest resetPasswordRequest) {
        try {
            boolean success = authenticationService.resetPassword(resetPasswordRequest);
            if (success) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(new ResponseObject<>(HttpStatus.OK.value(), "Đặt lại mật khẩu thành công", "Success"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Lỗi đặt lại mật khẩu", null));
            }
        } catch (CustomException e) {
            return ResponseEntity.status(e.getStatus())
                    .body(new ResponseObject<>(e.getStatus().value(), e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseObject<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Lỗi bất ngờ xảy ra", null));
        }
    }
}

