package com.revplay.musicplatform.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.audit.enums.AuditActionType;
import com.revplay.musicplatform.audit.enums.AuditEntityType;
import com.revplay.musicplatform.audit.service.AuditLogService;
import com.revplay.musicplatform.security.AuthenticatedUserPrincipal;
import com.revplay.musicplatform.security.JwtProperties;
import com.revplay.musicplatform.security.service.InMemoryRateLimiterService;
import com.revplay.musicplatform.security.service.JwtService;
import com.revplay.musicplatform.security.service.TokenRevocationService;
import com.revplay.musicplatform.user.dto.request.ChangePasswordRequest;
import com.revplay.musicplatform.user.dto.request.ForgotPasswordRequest;
import com.revplay.musicplatform.user.dto.request.LoginRequest;
import com.revplay.musicplatform.user.dto.request.RefreshTokenRequest;
import com.revplay.musicplatform.user.dto.request.RegisterRequest;
import com.revplay.musicplatform.user.dto.request.ResetPasswordRequest;
import com.revplay.musicplatform.user.dto.response.AuthTokenResponse;
import com.revplay.musicplatform.user.dto.response.SimpleMessageResponse;
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
import com.revplay.musicplatform.user.service.EmailService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AuthServiceImplTest {

    private static final String EMAIL = "listener@revplay.com";
    private static final String USERNAME = "listener";
    private static final String FULL_NAME = "Listener User";
    private static final String RAW_PASSWORD = "StrongPass@123";
    private static final String ENCODED_PASSWORD = "encoded-password";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String CLIENT_KEY = "10.0.0.1";
    private static final String OTP = "123456";
    private static final String RESET_TOKEN = "reset-token";

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private TokenRevocationService tokenRevocationService;
    @Mock
    private InMemoryRateLimiterService inMemoryRateLimiterService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private EmailService emailService;
    @Captor
    private ArgumentCaptor<User> userCaptor;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha");
        jwtProperties.setAccessTokenExpirationSeconds(3600L);
        jwtProperties.setRefreshTokenExpirationSeconds(1209600L);
        authService = new AuthServiceImpl(
                userRepository,
                userProfileRepository,
                passwordResetTokenRepository,
                passwordEncoder,
                jwtService,
                jwtProperties,
                tokenRevocationService,
                inMemoryRateLimiterService,
                auditLogService,
                emailService
        );
    }

    @Test
    @DisplayName("register happy path saves user and profile and sends OTP email")
    void registerHappyPath() {
        RegisterRequest request = new RegisterRequest(EMAIL, USERNAME, RAW_PASSWORD, FULL_NAME, UserRole.LISTENER.name());
        User saved = verifiedActiveUser(1L);
        saved.setEmailVerified(Boolean.FALSE);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(userRepository.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));
        mockTokenFlow(saved);

        AuthTokenResponse response = authService.register(request);

        verify(userRepository, times(2)).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmailOtp()).isNotBlank();
        verify(emailService).sendEmail(eq(EMAIL), eq("Verify your RevPlay account"), any(String.class));
        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
    }

    @Test
    @DisplayName("register throws on null request")
    void registerNullRequest() {
        assertThatThrownBy(() -> authService.register(null))
                .isInstanceOf(AuthValidationException.class)
                .hasMessage("Register request is required");
    }

    @Test
    @DisplayName("register throws weak password validation")
    void registerWeakPassword() {
        RegisterRequest request = new RegisterRequest(EMAIL, USERNAME, "myPassword@1", FULL_NAME, null);
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(AuthValidationException.class)
                .hasMessage("Password is too weak");
    }

    @Test
    @DisplayName("register throws conflict when email exists")
    void registerEmailExists() {
        RegisterRequest request = new RegisterRequest(EMAIL, USERNAME, RAW_PASSWORD, FULL_NAME, null);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(verifiedActiveUser(2L)));
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(AuthConflictException.class)
                .hasMessage("Email already exists");
    }

    @Test
    @DisplayName("register resolves artist role when ARTIST role requested")
    void registerArtistRoleRequested() {
        RegisterRequest request = new RegisterRequest(EMAIL, USERNAME, RAW_PASSWORD, FULL_NAME, UserRole.ARTIST.name());
        User saved = verifiedActiveUser(22L);
        saved.setRole(UserRole.ARTIST);
        saved.setEmailVerified(Boolean.FALSE);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(userRepository.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(userProfileRepository.findByUserId(22L)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));
        mockTokenFlow(saved);

        authService.register(request);

        verify(userRepository, times(2)).save(userCaptor.capture());
        assertThat(userCaptor.getAllValues().get(0).getRole()).isEqualTo(UserRole.ARTIST);
    }

    @Test
    @DisplayName("register throws conflict when username already exists")
    void registerUsernameExists() {
        RegisterRequest request = new RegisterRequest(EMAIL, USERNAME, RAW_PASSWORD, FULL_NAME, null);
        User existingInactiveByEmail = verifiedActiveUser(100L);
        existingInactiveByEmail.setIsActive(Boolean.FALSE);
        User existingByUsername = verifiedActiveUser(101L);
        existingByUsername.setUsername(USERNAME);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(existingInactiveByEmail));
        when(userRepository.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(existingByUsername));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(AuthConflictException.class)
                .hasMessage("Username already exists");
    }

    @Test
    @DisplayName("register swallows email exception and still returns token")
    void registerEmailExceptionSwallowed() {
        RegisterRequest request = new RegisterRequest(EMAIL, USERNAME, RAW_PASSWORD, FULL_NAME, null);
        User saved = verifiedActiveUser(102L);
        saved.setEmailVerified(Boolean.FALSE);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(userRepository.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(userProfileRepository.findByUserId(102L)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("mail-fail")).when(emailService).sendEmail(eq(EMAIL), any(String.class), any(String.class));
        mockTokenFlow(saved);

        AuthTokenResponse response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
    }

    @Test
    @DisplayName("login happy path via email")
    void loginByEmail() {
        User user = verifiedActiveUser(3L);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, user.getPasswordHash())).thenReturn(true);
        mockTokenFlow(user);

        AuthTokenResponse response = authService.login(new LoginRequest(EMAIL, RAW_PASSWORD), CLIENT_KEY);

        verify(inMemoryRateLimiterService).ensureWithinLimit("login:" + CLIENT_KEY, 5, 60, "Too many login attempts. Please try again later.");
        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
    }

    @Test
    @DisplayName("login throws invalid credentials when user missing")
    void loginUserMissing() {
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, RAW_PASSWORD), CLIENT_KEY))
                .isInstanceOf(AuthUnauthorizedException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    @DisplayName("login happy path via username when no at-sign present")
    void loginByUsername() {
        User user = verifiedActiveUser(23L);
        when(userRepository.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, user.getPasswordHash())).thenReturn(true);
        mockTokenFlow(user);

        AuthTokenResponse response = authService.login(new LoginRequest(USERNAME, RAW_PASSWORD), CLIENT_KEY);

        assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
        verify(userRepository).findByUsernameIgnoreCase(USERNAME);
    }

    @Test
    @DisplayName("login throws unauthorized when email not verified")
    void loginEmailNotVerified() {
        User user = verifiedActiveUser(24L);
        user.setEmailVerified(Boolean.FALSE);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, RAW_PASSWORD), CLIENT_KEY))
                .isInstanceOf(AuthUnauthorizedException.class)
                .hasMessage("Please verify your email before logging in.");
    }

    @Test
    @DisplayName("login throws unauthorized when account is deactivated")
    void loginInactiveUser() {
        User user = verifiedActiveUser(25L);
        user.setIsActive(Boolean.FALSE);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, RAW_PASSWORD), CLIENT_KEY))
                .isInstanceOf(AuthUnauthorizedException.class)
                .hasMessage("Account is deactivated");
    }

    @Test
    @DisplayName("login throws unauthorized when password does not match")
    void loginWrongPassword() {
        User user = verifiedActiveUser(26L);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, RAW_PASSWORD), CLIENT_KEY))
                .isInstanceOf(AuthUnauthorizedException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    @DisplayName("refresh token happy path for active user")
    void refreshTokenHappyPath() {
        User user = verifiedActiveUser(4L);
        when(tokenRevocationService.isRevoked(REFRESH_TOKEN)).thenReturn(false);
        when(jwtService.isRefreshToken(REFRESH_TOKEN)).thenReturn(true);
        when(jwtService.toPrincipal(REFRESH_TOKEN)).thenReturn(new AuthenticatedUserPrincipal(4L, USERNAME, UserRole.LISTENER));
        when(userRepository.findById(4L)).thenReturn(Optional.of(user));
        mockTokenFlow(user);

        AuthTokenResponse response = authService.refreshToken(new RefreshTokenRequest(REFRESH_TOKEN));
        assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
    }

    @Test
    @DisplayName("refresh token revoked throws unauthorized")
    void refreshTokenRevoked() {
        when(tokenRevocationService.isRevoked(REFRESH_TOKEN)).thenReturn(true);
        assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest(REFRESH_TOKEN)))
                .isInstanceOf(AuthUnauthorizedException.class)
                .hasMessage("Refresh token is revoked");
    }

    @Test
    @DisplayName("refresh with non-refresh token throws unauthorized")
    void refreshTokenNotRefresh() {
        when(tokenRevocationService.isRevoked(REFRESH_TOKEN)).thenReturn(false);
        when(jwtService.isRefreshToken(REFRESH_TOKEN)).thenReturn(false);
        assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest(REFRESH_TOKEN)))
                .isInstanceOf(AuthUnauthorizedException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    @DisplayName("refresh throws unauthorized when user not found")
    void refreshTokenUserNotFound() {
        when(tokenRevocationService.isRevoked(REFRESH_TOKEN)).thenReturn(false);
        when(jwtService.isRefreshToken(REFRESH_TOKEN)).thenReturn(true);
        when(jwtService.toPrincipal(REFRESH_TOKEN)).thenReturn(new AuthenticatedUserPrincipal(999L, USERNAME, UserRole.LISTENER));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest(REFRESH_TOKEN)))
                .isInstanceOf(AuthUnauthorizedException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("refresh throws unauthorized when user is inactive")
    void refreshTokenInactiveUser() {
        User user = verifiedActiveUser(27L);
        user.setIsActive(Boolean.FALSE);
        when(tokenRevocationService.isRevoked(REFRESH_TOKEN)).thenReturn(false);
        when(jwtService.isRefreshToken(REFRESH_TOKEN)).thenReturn(true);
        when(jwtService.toPrincipal(REFRESH_TOKEN)).thenReturn(new AuthenticatedUserPrincipal(27L, USERNAME, UserRole.LISTENER));
        when(userRepository.findById(27L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest(REFRESH_TOKEN)))
                .isInstanceOf(AuthUnauthorizedException.class)
                .hasMessage("Account is deactivated");
    }

    @Test
    @DisplayName("logout with null token is idempotent")
    void logoutNullToken() {
        SimpleMessageResponse response = authService.logout(null);
        verify(tokenRevocationService, never()).revoke(any(), any());
        assertThat(response.message()).isEqualTo("Logged out successfully");
    }

    @Test
    @DisplayName("logout with token revokes and returns success")
    void logoutWithToken() {
        Instant expiry = Instant.now().plusSeconds(120);
        when(jwtService.getExpiry(ACCESS_TOKEN)).thenReturn(expiry);

        SimpleMessageResponse response = authService.logout(ACCESS_TOKEN);

        verify(tokenRevocationService).revoke(ACCESS_TOKEN, expiry);
        assertThat(response.message()).isEqualTo("Logged out successfully");
    }

    @Test
    @DisplayName("logout with blank token does not revoke")
    void logoutBlankToken() {
        SimpleMessageResponse response = authService.logout("  ");
        verify(tokenRevocationService, never()).revoke(any(), any());
        assertThat(response.message()).isEqualTo("Logged out successfully");
    }

    @Test
    @DisplayName("forgot password happy path cleans old tokens and sends email")
    void forgotPasswordHappyPath() {
        User user = verifiedActiveUser(5L);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(i -> i.getArgument(0));

        SimpleMessageResponse response = authService.forgotPassword(new ForgotPasswordRequest(EMAIL), CLIENT_KEY);

        verify(inMemoryRateLimiterService).ensureWithinLimit("forgot-password:" + CLIENT_KEY, 3, 600, "Too many forgot-password requests. Please try again later.");
        verify(passwordResetTokenRepository).deleteByUser(user);
        verify(emailService).sendEmail(eq(EMAIL), eq("Reset your RevPlay password"), any(String.class));
        assertThat(response.message()).isEqualTo("Password reset email sent successfully");
    }

    @Test
    @DisplayName("forgot password throws not found for unknown email")
    void forgotPasswordUserNotFound() {
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.forgotPassword(new ForgotPasswordRequest(EMAIL), CLIENT_KEY))
                .isInstanceOf(AuthNotFoundException.class)
                .hasMessage("User not found for the given email");
    }

    @Test
    @DisplayName("forgot password swallows email send exceptions")
    void forgotPasswordEmailThrows() {
        User user = verifiedActiveUser(28L);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("mail-fail")).when(emailService).sendEmail(eq(EMAIL), any(String.class), any(String.class));

        SimpleMessageResponse response = authService.forgotPassword(new ForgotPasswordRequest(EMAIL), CLIENT_KEY);

        assertThat(response.message()).isEqualTo("Password reset email sent successfully");
    }

    @Test
    @DisplayName("reset password deletes expired token and throws validation")
    void resetPasswordExpiredToken() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(RESET_TOKEN);
        token.setExpiryDate(Instant.now().minusSeconds(1));
        token.setUser(verifiedActiveUser(6L));
        when(passwordResetTokenRepository.findByToken(RESET_TOKEN)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest(RESET_TOKEN, "NewPass@123")))
                .isInstanceOf(AuthValidationException.class)
                .hasMessage("Reset token is expired");
        verify(passwordResetTokenRepository).delete(token);
    }

    @Test
    @DisplayName("reset password happy path updates hash deletes token and logs")
    void resetPasswordHappyPath() {
        User user = verifiedActiveUser(29L);
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(RESET_TOKEN);
        token.setUser(user);
        token.setExpiryDate(Instant.now().plusSeconds(120));
        when(passwordResetTokenRepository.findByToken(RESET_TOKEN)).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewPass@123")).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        SimpleMessageResponse response = authService.resetPassword(new ResetPasswordRequest(RESET_TOKEN, "NewPass@123"));

        verify(passwordResetTokenRepository).delete(token);
        verify(auditLogService).logInternal(AuditActionType.PASSWORD_RESET, 29L, AuditEntityType.USER, 29L, "Password reset completed via reset token");
        assertThat(response.message()).isEqualTo("Password reset successful");
    }

    @Test
    @DisplayName("reset password invalid token throws validation")
    void resetPasswordInvalidToken() {
        when(passwordResetTokenRepository.findByToken(RESET_TOKEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest(RESET_TOKEN, "NewPass@123")))
                .isInstanceOf(AuthValidationException.class)
                .hasMessage("Invalid reset token");
    }

    @Test
    @DisplayName("change password happy path updates hash and logs audit")
    void changePasswordHappyPath() {
        User user = verifiedActiveUser(7L);
        user.setPasswordHash("old-hash");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass@123", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("NewPass@123")).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        SimpleMessageResponse response = authService.changePassword(7L, new ChangePasswordRequest("OldPass@123", "NewPass@123"));

        verify(auditLogService).logInternal(AuditActionType.PASSWORD_CHANGE, 7L, AuditEntityType.USER, 7L, "Password changed by authenticated user");
        assertThat(response.message()).isEqualTo("Password changed successfully");
    }

    @Test
    @DisplayName("change password throws not found when user missing")
    void changePasswordUserNotFound() {
        when(userRepository.findById(700L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.changePassword(700L, new ChangePasswordRequest("OldPass@123", "NewPass@123")))
                .isInstanceOf(AuthNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("change password throws validation for wrong current password")
    void changePasswordWrongCurrentPassword() {
        User user = verifiedActiveUser(701L);
        user.setPasswordHash("old-hash");
        when(userRepository.findById(701L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Wrong@123", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(701L, new ChangePasswordRequest("Wrong@123", "NewPass@123")))
                .isInstanceOf(AuthValidationException.class)
                .hasMessage("Current password is incorrect");
    }

    @Test
    @DisplayName("verify email otp happy path sets email verified and clears otp")
    void verifyEmailOtpHappyPath() {
        User user = verifiedActiveUser(8L);
        user.setEmailVerified(Boolean.FALSE);
        user.setEmailOtp(OTP);
        user.setOtpExpiryTime(LocalDateTime.now().plusMinutes(5));
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        SimpleMessageResponse response = authService.verifyEmailOtp(EMAIL, OTP);

        verify(emailService).sendWelcomeEmail(EMAIL, USERNAME);
        assertThat(response.message()).isEqualTo("Email verified successfully");
    }

    @Test
    @DisplayName("verify email otp already verified returns message")
    void verifyEmailAlreadyVerified() {
        User user = verifiedActiveUser(30L);
        user.setEmailVerified(Boolean.TRUE);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));

        SimpleMessageResponse response = authService.verifyEmailOtp(EMAIL, OTP);

        assertThat(response.message()).isEqualTo("Email already verified");
    }

    @Test
    @DisplayName("verify email otp wrong code throws validation")
    void verifyEmailWrongOtp() {
        User user = verifiedActiveUser(31L);
        user.setEmailVerified(Boolean.FALSE);
        user.setEmailOtp(OTP);
        user.setOtpExpiryTime(LocalDateTime.now().plusMinutes(2));
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.verifyEmailOtp(EMAIL, "000000"))
                .isInstanceOf(AuthValidationException.class)
                .hasMessage("Invalid OTP");
    }

    @Test
    @DisplayName("verify email otp expired code throws validation")
    void verifyEmailExpiredOtp() {
        User user = verifiedActiveUser(32L);
        user.setEmailVerified(Boolean.FALSE);
        user.setEmailOtp(OTP);
        user.setOtpExpiryTime(LocalDateTime.now().minusSeconds(1));
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.verifyEmailOtp(EMAIL, OTP))
                .isInstanceOf(AuthValidationException.class)
                .hasMessage("OTP expired");
    }

    @Test
    @DisplayName("verify email otp swallows welcome email exception")
    void verifyEmailWelcomeThrows() {
        User user = verifiedActiveUser(33L);
        user.setEmailVerified(Boolean.FALSE);
        user.setEmailOtp(OTP);
        user.setOtpExpiryTime(LocalDateTime.now().plusMinutes(2));
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("welcome-fail")).when(emailService).sendWelcomeEmail(EMAIL, USERNAME);

        SimpleMessageResponse response = authService.verifyEmailOtp(EMAIL, OTP);

        assertThat(response.message()).isEqualTo("Email verified successfully");
    }

    @Test
    @DisplayName("resend otp swallows email exception and still succeeds")
    void resendOtpSwallowsEmailException() {
        User user = verifiedActiveUser(9L);
        user.setEmailVerified(Boolean.FALSE);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(new RuntimeException("mail-fail")).when(emailService).sendEmail(eq(EMAIL), any(String.class), any(String.class));

        SimpleMessageResponse response = authService.resendEmailOtp(EMAIL);

        assertThat(response.message()).isEqualTo("OTP sent successfully");
    }

    @Test
    @DisplayName("resend otp happy path sets otp and sends email")
    void resendOtpHappyPath() {
        User user = verifiedActiveUser(34L);
        user.setEmailVerified(Boolean.FALSE);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        SimpleMessageResponse response = authService.resendEmailOtp(EMAIL);

        verify(emailService).sendEmail(eq(EMAIL), eq("Verify your RevPlay account"), any(String.class));
        assertThat(user.getEmailOtp()).isNotBlank();
        assertThat(response.message()).isEqualTo("OTP sent successfully");
    }

    @Test
    @DisplayName("resend otp already verified returns message and does not save")
    void resendOtpAlreadyVerified() {
        User user = verifiedActiveUser(35L);
        user.setEmailVerified(Boolean.TRUE);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));

        SimpleMessageResponse response = authService.resendEmailOtp(EMAIL);

        verify(userRepository, never()).save(any(User.class));
        assertThat(response.message()).isEqualTo("Email already verified");
    }

    private void mockTokenFlow(User user) {
        when(jwtService.generateAccessToken(user)).thenReturn(ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(user)).thenReturn(REFRESH_TOKEN);
        when(jwtService.getExpiry(ACCESS_TOKEN)).thenReturn(Instant.now().plusSeconds(3600));
        when(jwtService.getExpiry(REFRESH_TOKEN)).thenReturn(Instant.now().plusSeconds(1209600));
        doNothing().when(tokenRevocationService).revokeAllForUser(user.getUserId());
        doNothing().when(tokenRevocationService).registerIssuedToken(eq(user.getUserId()), any(String.class), any(Instant.class));
    }

    private User verifiedActiveUser(Long userId) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail(EMAIL);
        user.setUsername(USERNAME);
        user.setRole(UserRole.LISTENER);
        user.setPasswordHash("old-hash");
        user.setIsActive(Boolean.TRUE);
        user.setEmailVerified(Boolean.TRUE);
        user.setCreatedAt(Instant.now().minusSeconds(60));
        user.setUpdatedAt(Instant.now().minusSeconds(30));
        return user;
    }
}
