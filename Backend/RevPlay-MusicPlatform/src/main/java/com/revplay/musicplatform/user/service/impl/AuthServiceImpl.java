package com.revplay.musicplatform.user.service.impl;


import com.revplay.musicplatform.security.JwtProperties;
import com.revplay.musicplatform.user.service.AuthService;
import com.revplay.musicplatform.user.service.EmailService;
import com.revplay.musicplatform.user.dto.request.ChangePasswordRequest;
import com.revplay.musicplatform.user.dto.request.ForgotPasswordRequest;
import com.revplay.musicplatform.user.dto.request.LoginRequest;
import com.revplay.musicplatform.user.dto.request.RefreshTokenRequest;
import com.revplay.musicplatform.user.dto.request.RegisterRequest;
import com.revplay.musicplatform.user.dto.request.ResetPasswordRequest;
import com.revplay.musicplatform.user.dto.response.AuthTokenResponse;
import com.revplay.musicplatform.user.dto.response.SimpleMessageResponse;
import com.revplay.musicplatform.user.dto.response.UserResponse;
import com.revplay.musicplatform.user.entity.PasswordResetToken;
import com.revplay.musicplatform.user.entity.User;
import com.revplay.musicplatform.user.entity.UserProfile;
import com.revplay.musicplatform.user.enums.UserRole;
import com.revplay.musicplatform.user.exception.AuthConflictException;
import com.revplay.musicplatform.user.exception.AuthNotFoundException;
import com.revplay.musicplatform.user.exception.AuthUnauthorizedException;
import com.revplay.musicplatform.user.exception.AuthValidationException;
import com.revplay.musicplatform.user.repository.PasswordResetTokenRepository;
import com.revplay.musicplatform.user.repository.UserProfileRepository;
import com.revplay.musicplatform.user.repository.UserRepository;
import com.revplay.musicplatform.user.util.OtpGeneratorUtil;
import com.revplay.musicplatform.audit.enums.AuditActionType;
import com.revplay.musicplatform.audit.enums.AuditEntityType;
import com.revplay.musicplatform.audit.service.AuditLogService;
import com.revplay.musicplatform.security.AuthenticatedUserPrincipal;
import com.revplay.musicplatform.security.service.InMemoryRateLimiterService;
import com.revplay.musicplatform.security.service.JwtService;
import com.revplay.musicplatform.security.service.TokenRevocationService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
    private static final long PASSWORD_RESET_TOKEN_EXPIRY_SECONDS = 30 * 60;

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final TokenRevocationService tokenRevocationService;
    private final InMemoryRateLimiterService inMemoryRateLimiterService;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    public AuthServiceImpl(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            TokenRevocationService tokenRevocationService,
            InMemoryRateLimiterService inMemoryRateLimiterService,
            AuditLogService auditLogService,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.tokenRevocationService = tokenRevocationService;
        this.inMemoryRateLimiterService = inMemoryRateLimiterService;
        this.auditLogService = auditLogService;
        this.emailService = emailService;
    }

    @Transactional
    public AuthTokenResponse register(RegisterRequest request) {
        LOGGER.info("Registering user with username={}", request == null ? null : request.username());
        validateRegisterRequest(request);

        String normalizedEmail = request.email().trim().toLowerCase();
        String normalizedUsername = request.username().trim();
        String normalizedFullName = request.fullName().trim();

        User existingByEmail = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (existingByEmail != null && Boolean.TRUE.equals(existingByEmail.getIsActive())) {
            throw new AuthConflictException("Email already exists");
        }

        User existingByUsername = userRepository.findByUsernameIgnoreCase(normalizedUsername).orElse(null);
        if (existingByUsername != null
                && (existingByEmail == null || !existingByUsername.getUserId().equals(existingByEmail.getUserId()))) {
            throw new AuthConflictException("Username already exists");
        }

        Instant now = Instant.now();
        User user = existingByEmail != null ? existingByEmail : new User();
        user.setEmail(normalizedEmail);
        user.setUsername(normalizedUsername);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(resolveRequestedRole(request.role()));
        user.setIsActive(Boolean.TRUE);
        user.setEmailVerified(Boolean.FALSE);
        user.setCreatedAt(existingByEmail == null ? now : user.getCreatedAt());
        user.setUpdatedAt(now);
        User savedUser = userRepository.save(user);

        UserProfile profile = userProfileRepository.findByUserId(savedUser.getUserId()).orElseGet(UserProfile::new);
        profile.setUserId(savedUser.getUserId());
        profile.setFullName(normalizedFullName);
        profile.setBio(null);
        profile.setProfilePictureUrl(null);
        profile.setCountry(null);
        profile.setCreatedAt(profile.getCreatedAt() == null ? now : profile.getCreatedAt());
        profile.setUpdatedAt(now);
        userProfileRepository.save(profile);

        String otp = OtpGeneratorUtil.generateOtp();
        savedUser.setEmailOtp(otp);
        savedUser.setOtpExpiryTime(LocalDateTime.now().plusMinutes(5));
        savedUser.setEmailVerified(Boolean.FALSE);
        userRepository.save(savedUser);

        try {
            emailService.sendEmail(
                    savedUser.getEmail(),
                    "Verify your RevPlay account",
                    """
                    Hi %s,

                    Thanks for signing up for RevPlay.

                    To activate your account, please verify your email using the code below.

                    Verification Code

                    %s

                    Important information:

                    - This code expires in 5 minutes
                    - Enter the code on the verification screen to complete your registration

                    Once verified, you can start listening on RevPlay.

                    RevPlay
                    """.formatted(resolveDisplayName(savedUser), otp)
            );
        } catch (Exception ex) {
            LOGGER.error("OTP email send failed for userId={}, email={}", savedUser.getUserId(), savedUser.getEmail(), ex);
        }

        return buildTokenResponse(savedUser);
    }

    public AuthTokenResponse login(LoginRequest request, String clientKey) {
        LOGGER.info("Processing login request for client={}", normalizeClientKey(clientKey));
        inMemoryRateLimiterService.ensureWithinLimit(
                "login:" + normalizeClientKey(clientKey),
                5,
                60,
                "Too many login attempts. Please try again later."
        );
        User user = resolveUserByUsernameOrEmail(request.usernameOrEmail())
                .orElseThrow(() -> new AuthUnauthorizedException("Invalid credentials"));
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new AuthUnauthorizedException("Please verify your email before logging in.");
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AuthUnauthorizedException("Account is deactivated");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthUnauthorizedException("Invalid credentials");
        }
        return buildTokenResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse refreshToken(RefreshTokenRequest request) {
        LOGGER.info("Refreshing token");
        if (tokenRevocationService.isRevoked(request.refreshToken())) {
            throw new AuthUnauthorizedException("Refresh token is revoked");
        }
        if (!jwtService.isRefreshToken(request.refreshToken())) {
            throw new AuthUnauthorizedException("Invalid refresh token");
        }
        AuthenticatedUserPrincipal principal = jwtService.toPrincipal(request.refreshToken());
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new AuthUnauthorizedException("User not found"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AuthUnauthorizedException("Account is deactivated");
        }
        return buildTokenResponse(user);
    }

    public SimpleMessageResponse logout(String bearerToken) {
        LOGGER.info("Processing logout request");
        if (bearerToken != null && !bearerToken.isBlank()) {
            tokenRevocationService.revoke(bearerToken, jwtService.getExpiry(bearerToken));
        }
        return new SimpleMessageResponse("Logged out successfully");
    }

    @Transactional
    public SimpleMessageResponse forgotPassword(ForgotPasswordRequest request, String clientKey) {
        LOGGER.info("Processing forgot-password for email={}", request == null ? null : request.email());
        inMemoryRateLimiterService.ensureWithinLimit(
                "forgot-password:" + normalizeClientKey(clientKey),
                3,
                600,
                "Too many forgot-password requests. Please try again later."
        );
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new AuthNotFoundException("User not found for the given email"));

        passwordResetTokenRepository.deleteByExpiryDateBefore(Instant.now());
        passwordResetTokenRepository.deleteByUser(user);

        PasswordResetToken tokenEntity = new PasswordResetToken();
        tokenEntity.setUser(user);
        tokenEntity.setToken(UUID.randomUUID().toString());
        tokenEntity.setExpiryDate(Instant.now().plusSeconds(PASSWORD_RESET_TOKEN_EXPIRY_SECONDS));
        tokenEntity.setCreatedAt(Instant.now());
        PasswordResetToken savedToken = passwordResetTokenRepository.save(tokenEntity);

        String resetLink = "http://localhost:4200/reset-password?token=" + savedToken.getToken();
        sendPasswordResetEmail(user.getEmail(), resetLink);

        return new SimpleMessageResponse("Password reset email sent successfully");
    }

    @Transactional
    public SimpleMessageResponse resetPassword(ResetPasswordRequest request) {
        LOGGER.info("Processing password reset by token");
        PasswordResetToken tokenEntity = passwordResetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new AuthValidationException("Invalid reset token"));
        if (tokenEntity.getExpiryDate().isBefore(Instant.now())) {
            passwordResetTokenRepository.delete(tokenEntity);
            throw new AuthValidationException("Reset token is expired");
        }

        User user = tokenEntity.getUser();

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        passwordResetTokenRepository.delete(tokenEntity);

        auditLogService.logInternal(
                AuditActionType.PASSWORD_RESET,
                user.getUserId(),
                AuditEntityType.USER,
                user.getUserId(),
                "Password reset completed via reset token"
        );

        return new SimpleMessageResponse("Password reset successful");
    }

    @Transactional
    public SimpleMessageResponse changePassword(Long userId, ChangePasswordRequest request) {
        LOGGER.info("Processing password change for userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new AuthValidationException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        auditLogService.logInternal(
                AuditActionType.PASSWORD_CHANGE,
                userId,
                AuditEntityType.USER,
                userId,
                "Password changed by authenticated user"
        );

        return new SimpleMessageResponse("Password changed successfully");
    }

    @Transactional
    public SimpleMessageResponse verifyEmailOtp(String email, String otp) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthNotFoundException("User not found for the given email"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return new SimpleMessageResponse("Email already verified");
        }
        if (user.getEmailOtp() == null || !user.getEmailOtp().equals(otp)) {
            throw new AuthValidationException("Invalid OTP");
        }
        if (user.getOtpExpiryTime() == null || user.getOtpExpiryTime().isBefore(LocalDateTime.now())) {
            throw new AuthValidationException("OTP expired");
        }

        user.setEmailVerified(Boolean.TRUE);
        user.setEmailOtp(null);
        user.setOtpExpiryTime(null);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        try {
            emailService.sendWelcomeEmail(user.getEmail(), resolveDisplayName(user));
        } catch (Exception ex) {
            LOGGER.error("Welcome email send failed after verification for userId={}, email={}", user.getUserId(), user.getEmail(), ex);
        }

        return new SimpleMessageResponse("Email verified successfully");
    }

    @Transactional
    public SimpleMessageResponse resendEmailOtp(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthNotFoundException("User not found for the given email"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return new SimpleMessageResponse("Email already verified");
        }

        String otp = OtpGeneratorUtil.generateOtp();
        user.setEmailOtp(otp);
        user.setOtpExpiryTime(LocalDateTime.now().plusMinutes(5));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        try {
            emailService.sendEmail(
                    user.getEmail(),
                    "Verify your RevPlay account",
                    """
                    Hi %s,

                    Thanks for signing up for RevPlay.

                    To activate your account, please verify your email using the code below.

                    Verification Code

                    %s

                    Important information:

                    - This code expires in 5 minutes
                    - Enter the code on the verification screen to complete your registration

                    Once verified, you can start listening on RevPlay.

                    RevPlay
                    """.formatted(resolveDisplayName(user), otp)
            );
        } catch (Exception ex) {
            LOGGER.error("OTP resend email failed for userId={}, email={}", user.getUserId(), user.getEmail(), ex);
        }

        return new SimpleMessageResponse("OTP sent successfully");
    }

    private AuthTokenResponse buildTokenResponse(User user) {
        tokenRevocationService.revokeAllForUser(user.getUserId());
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        tokenRevocationService.registerIssuedToken(user.getUserId(), accessToken, jwtService.getExpiry(accessToken));
        tokenRevocationService.registerIssuedToken(user.getUserId(), refreshToken, jwtService.getExpiry(refreshToken));

        return new AuthTokenResponse(
                "Bearer",
                accessToken,
                jwtProperties.getAccessTokenExpirationSeconds(),
                refreshToken,
                jwtProperties.getRefreshTokenExpirationSeconds(),
                toUserResponse(user)
        );
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getUsername(),
                user.getRole().name(),
                user.getIsActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private Optional<User> resolveUserByUsernameOrEmail(String usernameOrEmail) {
        String input = usernameOrEmail.trim();
        if (input.contains("@")) {
            return userRepository.findByEmailIgnoreCase(input);
        }
        return userRepository.findByUsernameIgnoreCase(input);
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (request == null) {
            throw new AuthValidationException("Register request is required");
        }
        if (request.password() != null && request.password().toLowerCase().contains("password")) {
            throw new AuthValidationException("Password is too weak");
        }
    }

    private void sendPasswordResetEmail(String toEmail, String resetLink) {
        try {
            emailService.sendEmail(
                    toEmail,
                    "Reset your RevPlay password",
                    """
                    Hi,

                    We received a request to reset your RevPlay password.

                    Use the secure link below to create a new password.

                    Reset your password

                    %s

                    For your security:

                    - This link expires in 15 minutes
                    - The link can only be used once
                    - If you did not request a reset, you can ignore this email

                    RevPlay Security
                    """.formatted(resetLink)
            );
        } catch (Exception ex) {
            LOGGER.error("Password reset email send failed for recipient={}", toEmail, ex);
        }
    }

    private String resolveDisplayName(User user) {
        if (user == null) {
            return "User";
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername().trim();
        }
        return "User";
    }

    private String normalizeClientKey(String clientKey) {
        return (clientKey == null || clientKey.isBlank()) ? "unknown" : clientKey.trim();
    }

    private UserRole resolveRequestedRole(String requestedRole) {
        if (requestedRole == null || requestedRole.isBlank()) {
            return UserRole.LISTENER;
        }
        return UserRole.from(requestedRole);
    }
}




