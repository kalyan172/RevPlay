package com.revplay.musicplatform.user.service.impl;

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
import com.revplay.musicplatform.user.service.UserAccountAdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class UserAccountAdminServiceImpl implements UserAccountAdminService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserAccountAdminServiceImpl.class);

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public UserAccountAdminServiceImpl(UserRepository userRepository, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponseDto<AdminUserDetailsResponse> listUsers(int page, int size) {
        LOGGER.info("Listing users for admin page={} size={}", page, size);
        Page<AdminUserDetailsResponse> userPage = userRepository.findAll(PageRequest.of(page, size))
                .map(this::toAdminUserDetailsResponse);
        return PagedResponseDto.of(userPage);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdminUserDetailsResponse> getUserById(Long userId) {
        LOGGER.info("Fetching admin user details for userId={}", userId);
        return userRepository.findById(userId)
                .map(this::toAdminUserDetailsResponse);
    }

    @Transactional
    public SimpleMessageResponse updateStatus(Long userId, UpdateUserStatusRequest request, AuthenticatedUserPrincipal admin) {
        LOGGER.info("Updating account status for userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthNotFoundException("User not found"));

        user.setIsActive(request.isActive());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        auditLogService.logInternal(
                AuditActionType.ACCOUNT_STATUS_UPDATE,
                admin.userId(),
                AuditEntityType.USER,
                userId,
                "is_active set to " + request.isActive()
        );
        return new SimpleMessageResponse("Account status updated");
    }

    @Transactional
    public SimpleMessageResponse updateRole(Long userId, UpdateUserRoleRequest request, AuthenticatedUserPrincipal admin) {
        LOGGER.info("Updating role for userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthNotFoundException("User not found"));

        UserRole oldRole = user.getRole();
        UserRole newRole = UserRole.from(request.role());
        user.setRole(newRole);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        auditLogService.logInternal(
                AuditActionType.ROLE_CHANGED,
                admin.userId(),
                AuditEntityType.USER,
                userId,
                "Role changed from " + oldRole.name() + " to " + newRole.name()
        );
        return new SimpleMessageResponse("User role updated");
    }

    private AdminUserDetailsResponse toAdminUserDetailsResponse(User user) {
        return new AdminUserDetailsResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                Boolean.TRUE.equals(user.getIsActive()) ? "ACTIVE" : "INACTIVE",
                user.getCreatedAt()
        );
    }
}


