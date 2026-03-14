package com.revplay.musicplatform.analytics.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.ads.repository.AdImpressionRepository;
import com.revplay.musicplatform.analytics.dto.response.BusinessOverviewResponse;
import com.revplay.musicplatform.analytics.dto.response.ConversionRateResponse;
import com.revplay.musicplatform.analytics.dto.response.RevenueAnalyticsResponse;
import com.revplay.musicplatform.analytics.dto.response.TopDownloadResponse;
import com.revplay.musicplatform.analytics.dto.response.TopMixResponse;
import com.revplay.musicplatform.download.repository.SongDownloadRepository;
import com.revplay.musicplatform.playback.repository.PlayHistoryRepository;
import com.revplay.musicplatform.premium.repository.SubscriptionPaymentRepository;
import com.revplay.musicplatform.premium.repository.UserSubscriptionRepository;
import com.revplay.musicplatform.systemplaylist.repository.SystemPlaylistRepository;
import com.revplay.musicplatform.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AdminBusinessAnalyticsServiceImplTest {

    private static final long TOTAL_USERS = 100L;
    private static final long TOTAL_AD_IMPRESSIONS = 250L;
    private static final long TOTAL_DOWNLOADS = 70L;
    private static final long TOTAL_PLAYS = 999L;
    private static final long ACTIVE_PREMIUM_USERS = 30L;
    private static final int LIMIT_NEGATIVE = -1;
    private static final int LIMIT_ZERO = 0;
    private static final int LIMIT_LARGE = 999;
    private static final int LIMIT_DEFAULT = 10;
    private static final int LIMIT_CAPPED = 100;
    private static final int LIMIT_VALID = 7;

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;
    @Mock
    private SubscriptionPaymentRepository subscriptionPaymentRepository;
    @Mock
    private AdImpressionRepository adImpressionRepository;
    @Mock
    private SongDownloadRepository songDownloadRepository;
    @Mock
    private PlayHistoryRepository playHistoryRepository;
    @Mock
    private SystemPlaylistRepository systemPlaylistRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private AdminBusinessAnalyticsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminBusinessAnalyticsServiceImpl(
                userRepository,
                userSubscriptionRepository,
                subscriptionPaymentRepository,
                adImpressionRepository,
                songDownloadRepository,
                playHistoryRepository,
                systemPlaylistRepository,
                jdbcTemplate);
    }

    @Test
    @DisplayName("getBusinessOverview returns aggregated counts")
    void getBusinessOverviewReturnsAggregatedCounts() {
        when(userRepository.count()).thenReturn(TOTAL_USERS);
        when(adImpressionRepository.count()).thenReturn(TOTAL_AD_IMPRESSIONS);
        when(songDownloadRepository.count()).thenReturn(TOTAL_DOWNLOADS);
        when(playHistoryRepository.count()).thenReturn(TOTAL_PLAYS);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), any(Object.class)))
                .thenReturn(ACTIVE_PREMIUM_USERS);

        BusinessOverviewResponse response = service.getBusinessOverview();

        assertThat(response.totalUsers()).isEqualTo(TOTAL_USERS);
        assertThat(response.activePremiumUsers()).isEqualTo(ACTIVE_PREMIUM_USERS);
        assertThat(response.totalAdImpressions()).isEqualTo(TOTAL_AD_IMPRESSIONS);
        assertThat(response.totalDownloads()).isEqualTo(TOTAL_DOWNLOADS);
        assertThat(response.totalSongPlays()).isEqualTo(TOTAL_PLAYS);
    }

    @Test
    @DisplayName("getBusinessOverview handles null premium count as zero")
    void getBusinessOverviewHandlesNullPremiumCount() {
        when(userRepository.count()).thenReturn(TOTAL_USERS);
        when(adImpressionRepository.count()).thenReturn(TOTAL_AD_IMPRESSIONS);
        when(songDownloadRepository.count()).thenReturn(TOTAL_DOWNLOADS);
        when(playHistoryRepository.count()).thenReturn(TOTAL_PLAYS);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), any(Object.class))).thenReturn(null);

        BusinessOverviewResponse response = service.getBusinessOverview();

        assertThat(response.activePremiumUsers()).isZero();
    }

    @Test
    @DisplayName("getRevenueAnalytics returns monthly yearly and total values")
    void getRevenueAnalyticsReturnsValues() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Double.class), any(Object.class), any(Object.class)))
                .thenReturn(1200.5, 5000.75);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Double.class))).thenReturn(10000.25);

        RevenueAnalyticsResponse response = service.getRevenueAnalytics();

        assertThat(response.monthlyRevenue()).isEqualTo(1200.5);
        assertThat(response.yearlyRevenue()).isEqualTo(5000.75);
        assertThat(response.totalRevenue()).isEqualTo(10000.25);
    }

    @Test
    @DisplayName("getRevenueAnalytics converts null query results to zero")
    void getRevenueAnalyticsConvertsNullToZero() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Double.class), any(), any()))
                .thenReturn(null, null);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Double.class))).thenReturn(null);

        RevenueAnalyticsResponse response = service.getRevenueAnalytics();

        assertThat(response.monthlyRevenue()).isZero();
        assertThat(response.yearlyRevenue()).isZero();
        assertThat(response.totalRevenue()).isZero();
    }

    @Test
    @DisplayName("getTopDownloadedSongs uses default limit when input is non-positive")
    @SuppressWarnings("unchecked")
    void getTopDownloadedSongsUsesDefaultLimit() {
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq(LIMIT_DEFAULT)))
                .thenReturn(List.of(new TopDownloadResponse(1L, 10L)));

        List<TopDownloadResponse> fromNegative = service.getTopDownloadedSongs(LIMIT_NEGATIVE);
        List<TopDownloadResponse> fromZero = service.getTopDownloadedSongs(LIMIT_ZERO);

        assertThat(fromNegative).hasSize(1);
        assertThat(fromZero).hasSize(1);
    }

    @Test
    @DisplayName("getTopDownloadedSongs caps limit to max 100")
    @SuppressWarnings("unchecked")
    void getTopDownloadedSongsCapsLimit() {
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq(LIMIT_CAPPED)))
                .thenReturn(List.of(new TopDownloadResponse(2L, 20L)));

        List<TopDownloadResponse> response = service.getTopDownloadedSongs(LIMIT_LARGE);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().songId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getTopDownloadedSongs uses provided limit when valid")
    @SuppressWarnings("unchecked")
    void getTopDownloadedSongsUsesProvidedLimit() {
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq(LIMIT_VALID)))
                .thenReturn(List.of(new TopDownloadResponse(3L, 30L)));

        List<TopDownloadResponse> response = service.getTopDownloadedSongs(LIMIT_VALID);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().downloadCount()).isEqualTo(30L);
    }

    @Test
    @DisplayName("getTopMixes returns mapped mix rows")
    @SuppressWarnings("unchecked")
    void getTopMixesReturnsRows() {
        TopMixResponse mix = new TopMixResponse("Mix-1", 55L);
        when(jdbcTemplate.query(any(String.class), any(RowMapper.class))).thenReturn(List.of(mix));

        List<TopMixResponse> response = service.getTopMixes();

        assertThat(response).containsExactly(mix);
    }

    @Test
    @DisplayName("getPremiumConversionRate computes rounded percentage")
    void getPremiumConversionRateComputesPercentage() {
        when(userRepository.count()).thenReturn(3L);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), any(Object.class))).thenReturn(1L);

        ConversionRateResponse response = service.getPremiumConversionRate();

        assertThat(response.percentage()).isEqualTo(33.33);
    }

    @Test
    @DisplayName("getPremiumConversionRate returns zero when total users is zero")
    void getPremiumConversionRateReturnsZeroWhenNoUsers() {
        when(userRepository.count()).thenReturn(0L);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), any(Object.class))).thenReturn(0L);

        ConversionRateResponse response = service.getPremiumConversionRate();

        assertThat(response.percentage()).isZero();
    }
}
