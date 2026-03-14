package com.revplay.musicplatform.premium.service;

import com.revplay.musicplatform.premium.dto.PremiumStatusResponse;

public interface SubscriptionService {

    boolean isUserPremium(Long userId);

    void upgradeToPremium(Long userId, String planType);

    PremiumStatusResponse getPremiumStatus(Long userId);
}

