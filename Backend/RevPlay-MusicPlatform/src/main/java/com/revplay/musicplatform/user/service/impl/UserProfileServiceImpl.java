package com.revplay.musicplatform.user.service.impl;

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
import com.revplay.musicplatform.user.service.UserProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserProfileServiceImpl.class);

    private final UserProfileRepository userProfileRepository;
    private final AuditLogService auditLogService;

    public UserProfileServiceImpl(UserProfileRepository userProfileRepository, AuditLogService auditLogService) {
        this.userProfileRepository = userProfileRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId, AuthenticatedUserPrincipal principal) {
        LOGGER.info("Fetching profile for userId={}", userId);
        ensureSelfOrAdmin(userId, principal);
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AuthNotFoundException("Profile not found for user " + userId));
        return toResponse(profile);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request, AuthenticatedUserPrincipal principal) {
        LOGGER.info("Updating profile for userId={}", userId);
        ensureSelfOrAdmin(userId, principal);
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AuthNotFoundException("Profile not found for user " + userId));

        profile.setFullName(request.fullName().trim());
        profile.setBio(request.bio());
        profile.setProfilePictureUrl(request.profilePictureUrl());
        profile.setCountry(request.country());
        profile.setUpdatedAt(Instant.now());

        UserProfile saved = userProfileRepository.save(profile);
        auditLogService.logInternal(
                AuditActionType.PROFILE_UPDATE,
                principal.userId(),
                AuditEntityType.USER_PROFILE,
                userId,
                "Profile details updated"
        );
        return toResponse(saved);
    }

    private UserProfileResponse toResponse(UserProfile profile) {
        return new UserProfileResponse(
                profile.getUserId(),
                profile.getFullName(),
                profile.getBio(),
                profile.getProfilePictureUrl(),
                profile.getCountry()
        );
    }

    private void ensureSelfOrAdmin(Long requestedUserId, AuthenticatedUserPrincipal principal) {
        boolean isAdmin = UserRole.ADMIN.name().equals(principal.role().name());
        if (!isAdmin && !requestedUserId.equals(principal.userId())) {
            throw new AuthForbiddenException("You can only access your own profile");
        }
    }
}


