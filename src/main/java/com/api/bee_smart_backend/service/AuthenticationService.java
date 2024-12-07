package com.api.bee_smart_backend.service;

import com.api.bee_smart_backend.helper.exception.CustomException;
import com.api.bee_smart_backend.helper.request.JwtRequest;
import com.api.bee_smart_backend.helper.request.ResetPasswordRequest;
import com.api.bee_smart_backend.helper.request.VerifyOtpRequest;
import com.api.bee_smart_backend.helper.response.JwtResponse;

public interface AuthenticationService {
    JwtResponse authenticate(JwtRequest authenticationRequest) throws CustomException;

    void logout(String tokenStr);

    String resendConfirmationEmail(String email);

    JwtResponse refreshToken(String refreshToken);

    boolean forgotPassword(String email);

    String verifyOtp(VerifyOtpRequest verifyOtpRequest);

    boolean resetPassword(ResetPasswordRequest resetPasswordRequest);
}
