package com.revplay.musicplatform.premium.controller;

import com.revplay.musicplatform.premium.dto.PremiumStatusResponse;
import com.revplay.musicplatform.premium.service.SubscriptionService;
import com.revplay.musicplatform.exception.AccessDeniedException;
import com.revplay.musicplatform.security.AuthenticatedUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/premium")
@RequiredArgsConstructor
@Tag(name = "Premium", description = "Premium subscription APIs")
public class PremiumController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PremiumController.class);

    private final SubscriptionService subscriptionService;

    @PostMapping("/upgrade")
    @Operation(summary = "Upgrade user to premium (dummy payment)")
    public ResponseEntity<Map<String, Object>> upgrade(
            @RequestParam Long userId,
            @RequestParam String planType,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        Long authenticatedUserId = principal.userId();
        if (!authenticatedUserId.equals(userId)) {
            LOGGER.warn("Rejected premium upgrade for mismatched userId param={}, authenticatedUserId={}", userId, authenticatedUserId);
            throw new AccessDeniedException("You can upgrade premium only for your own account.");
        }

        subscriptionService.upgradeToPremium(authenticatedUserId, planType);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Premium upgrade successful"
        ));
    }

    @GetMapping("/status")
    @Operation(summary = "Get premium status")
    public ResponseEntity<PremiumStatusResponse> getStatus(
            @RequestParam Long userId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        Long authenticatedUserId = principal.userId();
        if (!authenticatedUserId.equals(userId)) {
            LOGGER.warn("Rejected premium status for mismatched userId param={}, authenticatedUserId={}", userId, authenticatedUserId);
            throw new AccessDeniedException("You can view premium status only for your own account.");
        }

        return ResponseEntity.ok(subscriptionService.getPremiumStatus(authenticatedUserId));
    }
}
