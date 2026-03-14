package com.revplay.musicplatform.premium.dto;

import java.time.LocalDateTime;

public record PremiumStatusResponse(
        boolean isPremium,
        LocalDateTime expiryDate
) {
}

