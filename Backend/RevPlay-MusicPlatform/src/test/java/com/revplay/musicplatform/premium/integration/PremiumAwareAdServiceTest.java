package com.revplay.musicplatform.premium.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.ads.entity.Ad;
import com.revplay.musicplatform.ads.service.impl.AdServiceImpl;
import com.revplay.musicplatform.premium.service.SubscriptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PremiumAwareAdServiceTest {

    private static final Long USER_ID = 42L;
    private static final Long SONG_ID = 77L;

    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private AdServiceImpl delegate;

    @InjectMocks
    private PremiumAwareAdService premiumAwareAdService;

    @Test
    @DisplayName("premium user gets no ad and delegate is not called")
    void premiumUserGetsNoAd() {
        when(subscriptionService.isUserPremium(USER_ID)).thenReturn(true);

        Ad ad = premiumAwareAdService.getNextAd(USER_ID, SONG_ID);

        assertThat(ad).isNull();
    }

    @Test
    @DisplayName("non premium user delegates to ad service")
    void nonPremiumUserDelegatesToAdService() {
        Ad expectedAd = new Ad();
        expectedAd.setId(1L);
        when(subscriptionService.isUserPremium(USER_ID)).thenReturn(false);
        when(delegate.getNextAd(USER_ID, SONG_ID)).thenReturn(expectedAd);

        Ad ad = premiumAwareAdService.getNextAd(USER_ID, SONG_ID);

        assertThat(ad).isSameAs(expectedAd);
        verify(delegate).getNextAd(USER_ID, SONG_ID);
    }
}
