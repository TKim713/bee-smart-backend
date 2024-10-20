package com.api.bee_smart_backend.controller;

import com.api.bee_smart_backend.helper.request.CreateUserRequest;
import com.api.bee_smart_backend.helper.request.JwtRequest;
import com.api.bee_smart_backend.helper.response.CreateUserResponse;
import com.api.bee_smart_backend.helper.response.JwtResponse;
import com.api.bee_smart_backend.helper.response.ResponseObject;
import com.api.bee_smart_backend.service.AuthenticationService;
import com.api.bee_smart_backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "Authentication successful", jwtResponse));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ResponseObject<>(HttpStatus.UNAUTHORIZED.value(), "INVALID_CREDENTIALS", null));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ResponseObject<CreateUserResponse>> register(@RequestBody CreateUserRequest userRequest) {
        try {
            CreateUserResponse userResponse = userService.createUser(userRequest);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseObject<>(HttpStatus.OK.value(), "User created successfully", userResponse));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseObject<>(HttpStatus.BAD_REQUEST.value(), "Error creating user: " + e.getMessage(), null));
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestParam String token) {
        String message = userService.verifyEmail(token);
        return ResponseEntity.ok(message);
    }
}
