package com.revplay.musicplatform.user.controller;

import com.revplay.musicplatform.user.dto.request.ResendOtpRequest;
import com.revplay.musicplatform.user.dto.request.VerifyEmailOtpRequest;
import com.revplay.musicplatform.user.dto.response.SimpleMessageResponse;
import com.revplay.musicplatform.user.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication Verification", description = "Email OTP verification APIs")
public class AuthVerificationController {

    private final AuthService authService;

    public AuthVerificationController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/verify-email")
    public ResponseEntity<SimpleMessageResponse> verifyEmail(@Valid @RequestBody VerifyEmailOtpRequest request) {
        return ResponseEntity.ok(authService.verifyEmailOtp(request.email(), request.otp()));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<SimpleMessageResponse> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        return ResponseEntity.ok(authService.resendEmailOtp(request.email()));
    }
}

