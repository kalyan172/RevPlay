package com.revplay.musicplatform.user.controller;

import com.revplay.musicplatform.user.dto.request.UpdateUserRoleRequest;
import com.revplay.musicplatform.user.dto.request.UpdateUserStatusRequest;
import com.revplay.musicplatform.user.dto.response.AdminUserDetailsResponse;
import com.revplay.musicplatform.user.dto.response.SimpleMessageResponse;
import com.revplay.musicplatform.common.dto.PagedResponseDto;
import com.revplay.musicplatform.common.response.ApiResponse;
import com.revplay.musicplatform.security.AuthenticatedUserPrincipal;
import com.revplay.musicplatform.user.service.UserAccountAdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "User Administration", description = "Administrative user account management APIs")
public class UserAccountAdminController {

    private final UserAccountAdminService userAccountAdminService;

    public UserAccountAdminController(UserAccountAdminService userAccountAdminService) {
        this.userAccountAdminService = userAccountAdminService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponseDto<AdminUserDetailsResponse>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Users retrieved",
                userAccountAdminService.listUsers(page, size)
        ));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<AdminUserDetailsResponse> getUserById(@PathVariable Long userId) {
        return userAccountAdminService.getUserById(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<SimpleMessageResponse> updateStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return ResponseEntity.ok(userAccountAdminService.updateStatus(userId, request, principal));
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<SimpleMessageResponse> updateRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRoleRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return ResponseEntity.ok(userAccountAdminService.updateRole(userId, request, principal));
    }
}

