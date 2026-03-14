package com.revplay.musicplatform.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.audit.enums.AuditActionType;
import com.revplay.musicplatform.audit.enums.AuditEntityType;
import com.revplay.musicplatform.audit.service.AuditLogService;
import com.revplay.musicplatform.common.dto.PagedResponseDto;
import com.revplay.musicplatform.security.AuthenticatedUserPrincipal;
import com.revplay.musicplatform.user.dto.request.UpdateUserRoleRequest;
import com.revplay.musicplatform.user.dto.request.UpdateUserStatusRequest;
import com.revplay.musicplatform.user.dto.response.AdminUserDetailsResponse;
import com.revplay.musicplatform.user.dto.response.SimpleMessageResponse;
import com.revplay.musicplatform.user.entity.User;
import com.revplay.musicplatform.user.enums.UserRole;
import com.revplay.musicplatform.user.exception.AuthNotFoundException;
import com.revplay.musicplatform.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class UserAccountAdminServiceImplTest {

    private static final Long USER_ID = 21L;
    private static final Long ADMIN_ID = 1L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private UserAccountAdminServiceImpl userAccountAdminService;

    @Test
    @DisplayName("listUsers returns paged response with mapped admin details")
    void listUsersReturnsPage() {
        User user = user(USER_ID, UserRole.LISTENER, true);
        when(userRepository.findAll(PageRequest.of(0, 10))).thenReturn(new PageImpl<>(java.util.List.of(user), PageRequest.of(0, 10), 1));

        PagedResponseDto<AdminUserDetailsResponse> page = userAccountAdminService.listUsers(0, 10);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).role()).isEqualTo(UserRole.LISTENER.name());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getUserById returns mapped response when user exists")
    void getUserByIdFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID, UserRole.ARTIST, true)));

        Optional<AdminUserDetailsResponse> response = userAccountAdminService.getUserById(USER_ID);

        assertThat(response).isPresent();
        assertThat(response.orElseThrow().status()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("getUserById returns empty when user missing")
    void getUserByIdMissing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        Optional<AdminUserDetailsResponse> response = userAccountAdminService.getUserById(USER_ID);

        assertThat(response).isEmpty();
    }

    @Test
    @DisplayName("updateStatus updates isActive and logs audit")
    void updateStatusSuccess() {
        User existing = user(USER_ID, UserRole.LISTENER, true);
        AuthenticatedUserPrincipal admin = new AuthenticatedUserPrincipal(ADMIN_ID, "admin", UserRole.ADMIN);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));

        SimpleMessageResponse response = userAccountAdminService.updateStatus(USER_ID, new UpdateUserStatusRequest(false), admin);

        assertThat(response.message()).isEqualTo("Account status updated");
        assertThat(existing.getIsActive()).isFalse();
        verify(auditLogService).logInternal(
                eq(AuditActionType.ACCOUNT_STATUS_UPDATE),
                eq(ADMIN_ID),
                eq(AuditEntityType.USER),
                eq(USER_ID),
                eq("is_active set to false")
        );
    }

    @Test
    @DisplayName("updateStatus throws not found for unknown user")
    void updateStatusNotFound() {
        AuthenticatedUserPrincipal admin = new AuthenticatedUserPrincipal(ADMIN_ID, "admin", UserRole.ADMIN);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAccountAdminService.updateStatus(USER_ID, new UpdateUserStatusRequest(true), admin))
                .isInstanceOf(AuthNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("updateRole updates role using enum factory and logs audit")
    void updateRoleSuccess() {
        User existing = user(USER_ID, UserRole.LISTENER, true);
        AuthenticatedUserPrincipal admin = new AuthenticatedUserPrincipal(ADMIN_ID, "admin", UserRole.ADMIN);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existing));

        SimpleMessageResponse response = userAccountAdminService.updateRole(USER_ID, new UpdateUserRoleRequest("artist"), admin);

        assertThat(response.message()).isEqualTo("User role updated");
        assertThat(existing.getRole()).isEqualTo(UserRole.ARTIST);
        verify(auditLogService).logInternal(
                eq(AuditActionType.ROLE_CHANGED),
                eq(ADMIN_ID),
                eq(AuditEntityType.USER),
                eq(USER_ID),
                eq("Role changed from LISTENER to ARTIST")
        );
    }

    private User user(Long id, UserRole role, boolean active) {
        User user = new User();
        user.setUserId(id);
        user.setEmail("u" + id + "@revplay.com");
        user.setUsername("u" + id);
        user.setPasswordHash("hash");
        user.setRole(role);
        user.setIsActive(active);
        user.setEmailVerified(Boolean.TRUE);
        user.setCreatedAt(Instant.now().minusSeconds(120));
        user.setUpdatedAt(Instant.now().minusSeconds(60));
        return user;
    }
}
