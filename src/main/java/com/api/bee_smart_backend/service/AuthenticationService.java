package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.request.JwtRequest;
import com.api.bee_smart_backend.helper.response.JwtResponse;

public interface AuthenticationService {
    JwtResponse authenticate(JwtRequest authenticationRequest) throws Exception;
}
