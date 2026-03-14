package com.revplay.musicplatform.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.audit.enums.AuditActionType;
import com.revplay.musicplatform.audit.enums.AuditEntityType;
import com.revplay.musicplatform.audit.service.AuditLogService;
import com.revplay.musicplatform.security.AuthenticatedUserPrincipal;
import com.revplay.musicplatform.user.dto.request.UpdateProfileRequest;
import com.revplay.musicplatform.user.dto.response.UserProfileResponse;
import com.revplay.musicplatform.user.entity.UserProfile;
import com.revplay.musicplatform.user.enums.UserRole;
import com.revplay.musicplatform.user.exception.AuthForbiddenException;
import com.revplay.musicplatform.user.exception.AuthNotFoundException;
import com.revplay.musicplatform.user.repository.UserProfileRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class UserProfileServiceImplTest {

    private static final Long USER_ID = 10L;
    private static final Long ADMIN_ID = 1L;
    private static final Long OTHER_ID = 99L;
    private static final String FULL_NAME = "User One";
    private static final String NEW_NAME = "New Name";
    private static final String BIO = "bio";
    private static final String PIC = "pic";
    private static final String COUNTRY = "IN";
    private static final String FORBIDDEN_MSG = "You can only access your own profile";

    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private AuditLogService auditLogService;

    private UserProfileServiceImpl userProfileService;

    @BeforeEach
    void setUp() {
        userProfileService = new UserProfileServiceImpl(userProfileRepository, auditLogService);
    }

    @Test
    @DisplayName("getProfile returns data for self")
    void getProfileSelf() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(USER_ID, "user", UserRole.LISTENER);
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile(USER_ID, FULL_NAME)));

        UserProfileResponse response = userProfileService.getProfile(USER_ID, principal);

        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.fullName()).isEqualTo(FULL_NAME);
    }

    @Test
    @DisplayName("getProfile allows admin for any user")
    void getProfileAdmin() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(ADMIN_ID, "admin", UserRole.ADMIN);
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile(USER_ID, FULL_NAME)));

        UserProfileResponse response = userProfileService.getProfile(USER_ID, principal);

        assertThat(response.userId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("getProfile throws forbidden for non owner non admin")
    void getProfileForbidden() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(OTHER_ID, "other", UserRole.LISTENER);

        assertThatThrownBy(() -> userProfileService.getProfile(USER_ID, principal))
                .isInstanceOf(AuthForbiddenException.class)
                .hasMessage(FORBIDDEN_MSG);
    }

    @Test
    @DisplayName("getProfile throws not found when profile missing")
    void getProfileNotFound() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(USER_ID, "user", UserRole.LISTENER);
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.getProfile(USER_ID, principal))
                .isInstanceOf(AuthNotFoundException.class)
                .hasMessageContaining("Profile not found");
    }

    @Test
    @DisplayName("updateProfile updates fields trims full name and logs audit")
    void updateProfileSuccess() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(USER_ID, "user", UserRole.LISTENER);
        UserProfile existing = profile(USER_ID, "Old Name");
        UpdateProfileRequest request = new UpdateProfileRequest("  " + NEW_NAME + "  ", BIO, PIC, COUNTRY);
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = userProfileService.updateProfile(USER_ID, request, principal);

        assertThat(response.fullName()).isEqualTo(NEW_NAME);
        assertThat(response.bio()).isEqualTo(BIO);
        verify(auditLogService).logInternal(
                eq(AuditActionType.PROFILE_UPDATE),
                eq(USER_ID),
                eq(AuditEntityType.USER_PROFILE),
                eq(USER_ID),
                eq("Profile details updated"));
    }

    @Test
    @DisplayName("updateProfile throws forbidden for non owner")
    void updateProfileForbidden() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(OTHER_ID, "other", UserRole.LISTENER);
        UpdateProfileRequest request = new UpdateProfileRequest(NEW_NAME, null, null, null);

        assertThatThrownBy(() -> userProfileService.updateProfile(USER_ID, request, principal))
                .isInstanceOf(AuthForbiddenException.class)
                .hasMessage(FORBIDDEN_MSG);
    }

    private UserProfile profile(Long userId, String fullName) {
        UserProfile userProfile = new UserProfile();
        userProfile.setUserId(userId);
        userProfile.setFullName(fullName);
        userProfile.setCreatedAt(Instant.now().minusSeconds(60));
        userProfile.setUpdatedAt(Instant.now().minusSeconds(10));
        return userProfile;
    }
}
