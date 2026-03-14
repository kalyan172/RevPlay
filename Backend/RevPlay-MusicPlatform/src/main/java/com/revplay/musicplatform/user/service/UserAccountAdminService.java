package com.revplay.musicplatform.user.service;

import com.revplay.musicplatform.security.AuthenticatedUserPrincipal;
import com.revplay.musicplatform.common.dto.PagedResponseDto;
import com.revplay.musicplatform.user.dto.request.UpdateUserRoleRequest;
import com.revplay.musicplatform.user.dto.request.UpdateUserStatusRequest;
import com.revplay.musicplatform.user.dto.response.AdminUserDetailsResponse;
import com.revplay.musicplatform.user.dto.response.SimpleMessageResponse;

import java.util.Optional;

public interface UserAccountAdminService {

    PagedResponseDto<AdminUserDetailsResponse> listUsers(int page, int size);

    Optional<AdminUserDetailsResponse> getUserById(Long userId);

    SimpleMessageResponse updateStatus(Long userId, UpdateUserStatusRequest request, AuthenticatedUserPrincipal admin);

    SimpleMessageResponse updateRole(Long userId, UpdateUserRoleRequest request, AuthenticatedUserPrincipal admin);
}


