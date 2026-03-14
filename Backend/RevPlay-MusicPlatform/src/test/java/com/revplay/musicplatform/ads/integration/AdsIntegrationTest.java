package com.revplay.musicplatform.ads.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revplay.musicplatform.ads.entity.Ad;
import com.revplay.musicplatform.ads.repository.AdImpressionRepository;
import com.revplay.musicplatform.ads.repository.AdRepository;
import com.revplay.musicplatform.ads.repository.UserAdPlaybackStateRepository;
import com.revplay.musicplatform.catalog.service.DiscoveryPerformanceService;
import com.revplay.musicplatform.security.AuthenticatedUserPrincipal;
import com.revplay.musicplatform.user.enums.UserRole;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class AdsIntegrationTest {

    private static final String NEXT_AD_PATH = "/api/v1/ads/next";
    private static final String USER_ID_PARAM = "userId";
    private static final String SONG_ID_PARAM = "songId";
    private static final Long USER_ID = 9001L;
    private static final Long SONG_ID_1 = 101L;
    private static final Long SONG_ID_2 = 102L;
    private static final Long SONG_ID_3 = 103L;
    private static final Long SONG_ID_4 = 104L;
    private static final Long SONG_ID_5 = 105L;
    private static final Long SONG_ID_6 = 106L;
    private static final String AD_TITLE_1 = "Ad Alpha";
    private static final String AD_TITLE_2 = "Ad Beta";
    private static final String AD_MEDIA_1 = "/uploads/ads/ad-alpha.mp3";
    private static final String AD_MEDIA_2 = "/uploads/ads/ad-beta.mp3";
    private static final Integer AD_DURATION_SECONDS = 30;
    private static final Integer EXPECTED_FIRST_SCHEDULED_COUNT = 3;
    private static final Integer EXPECTED_SECOND_SCHEDULED_COUNT = 6;
    private static final long EXPECTED_IMPRESSIONS_AFTER_FIRST_SCHEDULE = 1L;
    private static final int EXPECTED_IMPRESSIONS_AFTER_TWO_SCHEDULES = 2;

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final AdRepository adRepository;
    private final AdImpressionRepository adImpressionRepository;
    private final UserAdPlaybackStateRepository userAdPlaybackStateRepository;

    @MockBean
    private DiscoveryPerformanceService discoveryPerformanceService;

    @Autowired
    AdsIntegrationTest(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            AdRepository adRepository,
            AdImpressionRepository adImpressionRepository,
            UserAdPlaybackStateRepository userAdPlaybackStateRepository
    ) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.adRepository = adRepository;
        this.adImpressionRepository = adImpressionRepository;
        this.userAdPlaybackStateRepository = userAdPlaybackStateRepository;
    }

    @BeforeEach
    void clean() {
        adImpressionRepository.deleteAll();
        userAdPlaybackStateRepository.deleteAll();
        adRepository.deleteAll();
    }

    @Test
    @DisplayName("scheduled ad is served on third song and impression is stored")
    void scheduledAdServedOnThirdSong() throws Exception {
        Ad ad = saveActiveAd(AD_TITLE_1, AD_MEDIA_1);

        JsonNode first = callNextAd(USER_ID, SONG_ID_1);
        JsonNode second = callNextAd(USER_ID, SONG_ID_2);
        JsonNode third = callNextAd(USER_ID, SONG_ID_3);

        assertThat(first.path("data").isMissingNode() || first.path("data").isNull()).isTrue();
        assertThat(second.path("data").isMissingNode() || second.path("data").isNull()).isTrue();
        assertThat(third.path("data").path("id").asLong()).isEqualTo(ad.getId());
        assertThat(adImpressionRepository.count()).isEqualTo(EXPECTED_IMPRESSIONS_AFTER_FIRST_SCHEDULE);
        assertThat(userAdPlaybackStateRepository.findByUserId(USER_ID)).isPresent();
        assertThat(userAdPlaybackStateRepository.findByUserId(USER_ID).orElseThrow().getSongsPlayedCount())
                .isEqualTo(EXPECTED_FIRST_SCHEDULED_COUNT);
    }

    @Test
    @DisplayName("ads rotate across scheduled slots when multiple active ads exist")
    void adsRotateAcrossScheduledSlots() throws Exception {
        Ad adOne = saveActiveAd(AD_TITLE_1, AD_MEDIA_1);
        Ad adTwo = saveActiveAd(AD_TITLE_2, AD_MEDIA_2);

        callNextAd(USER_ID, SONG_ID_1);
        callNextAd(USER_ID, SONG_ID_2);
        JsonNode third = callNextAd(USER_ID, SONG_ID_3);
        callNextAd(USER_ID, SONG_ID_4);
        callNextAd(USER_ID, SONG_ID_5);
        JsonNode sixth = callNextAd(USER_ID, SONG_ID_6);

        List<Long> servedIds = List.of(third.path("data").path("id").asLong(), sixth.path("data").path("id").asLong());
        assertThat(servedIds).containsExactly(adOne.getId(), adTwo.getId());
        assertThat(adImpressionRepository.count()).isEqualTo(EXPECTED_IMPRESSIONS_AFTER_TWO_SCHEDULES);
        assertThat(userAdPlaybackStateRepository.findByUserId(USER_ID).orElseThrow().getSongsPlayedCount())
                .isEqualTo(EXPECTED_SECOND_SCHEDULED_COUNT);
    }

    @Test
    @DisplayName("third song returns null ad when no active ad exists")
    void thirdSongReturnsNullWhenNoActiveAd() throws Exception {
        saveInactiveAd(AD_TITLE_1, AD_MEDIA_1);

        callNextAd(USER_ID, SONG_ID_1);
        callNextAd(USER_ID, SONG_ID_2);
        JsonNode third = callNextAd(USER_ID, SONG_ID_3);

        assertThat(third.path("data").isMissingNode() || third.path("data").isNull()).isTrue();
        assertThat(adImpressionRepository.count()).isZero();
    }

    @Test
    @DisplayName("next ad endpoint requires authentication")
    void nextAdEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get(NEXT_AD_PATH)
                        .param(USER_ID_PARAM, USER_ID.toString())
                        .param(SONG_ID_PARAM, SONG_ID_1.toString()))
                .andExpect(status().isForbidden());

        assertThat(adImpressionRepository.count()).isZero();
    }

    private JsonNode callNextAd(Long userId, Long songId) throws Exception {
        MvcResult result = mockMvc.perform(get(NEXT_AD_PATH)
                        .param(USER_ID_PARAM, userId.toString())
                        .param(SONG_ID_PARAM, songId.toString())
                        .with(authentication(auth(userId))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private Ad saveActiveAd(String title, String mediaUrl) {
        Ad ad = baseAd(title, mediaUrl);
        ad.setIsActive(true);
        return adRepository.save(ad);
    }

    private Ad saveInactiveAd(String title, String mediaUrl) {
        Ad ad = baseAd(title, mediaUrl);
        ad.setIsActive(false);
        return adRepository.save(ad);
    }

    private Ad baseAd(String title, String mediaUrl) {
        LocalDateTime now = LocalDateTime.now();
        Ad ad = new Ad();
        ad.setTitle(title);
        ad.setMediaUrl(mediaUrl);
        ad.setDurationSeconds(AD_DURATION_SECONDS);
        ad.setStartDate(now.minusDays(1));
        ad.setEndDate(now.plusDays(1));
        return ad;
    }

    private UsernamePasswordAuthenticationToken auth(Long userId) {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUserPrincipal(userId, "u" + userId, UserRole.LISTENER),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_LISTENER"))
        );
    }
}
