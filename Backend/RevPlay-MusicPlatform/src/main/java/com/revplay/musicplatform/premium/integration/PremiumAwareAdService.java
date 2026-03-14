package com.revplay.musicplatform.premium.integration;

import com.revplay.musicplatform.ads.entity.Ad;
import com.revplay.musicplatform.ads.service.AdService;
import com.revplay.musicplatform.ads.service.impl.AdServiceImpl;
import com.revplay.musicplatform.premium.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class PremiumAwareAdService implements AdService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PremiumAwareAdService.class);

    private final SubscriptionService subscriptionService;
    private final AdServiceImpl delegate;

    public PremiumAwareAdService(SubscriptionService subscriptionService, AdServiceImpl delegate) {
        this.subscriptionService = subscriptionService;
        this.delegate = delegate;
    }

    @Override
    public Ad getNextAd(Long userId, Long songId) {
        if (subscriptionService.isUserPremium(userId)) {
            LOGGER.debug("Skipping ad for premium userId={}", userId);
            return null;
        }
        return delegate.getNextAd(userId, songId);
    }
}

