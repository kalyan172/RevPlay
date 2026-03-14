package com.revplay.musicplatform.user.service;

import com.revplay.musicplatform.security.AuthenticatedUserPrincipal;
import com.revplay.musicplatform.user.dto.request.UpdateProfileRequest;
import com.revplay.musicplatform.user.dto.response.UserProfileResponse;

public interface UserProfileService {

    UserProfileResponse getProfile(Long userId, AuthenticatedUserPrincipal principal);

    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request, AuthenticatedUserPrincipal principal);
}


