package com.revplay.musicplatform.analytics.service.impl;

import com.revplay.musicplatform.ads.repository.AdImpressionRepository;
import com.revplay.musicplatform.analytics.dto.response.BusinessOverviewResponse;
import com.revplay.musicplatform.analytics.dto.response.ConversionRateResponse;
import com.revplay.musicplatform.analytics.dto.response.RevenueAnalyticsResponse;
import com.revplay.musicplatform.analytics.dto.response.TopDownloadResponse;
import com.revplay.musicplatform.analytics.dto.response.TopMixResponse;
import com.revplay.musicplatform.analytics.service.AdminBusinessAnalyticsService;
import com.revplay.musicplatform.download.repository.SongDownloadRepository;
import com.revplay.musicplatform.playback.repository.PlayHistoryRepository;
import com.revplay.musicplatform.premium.repository.SubscriptionPaymentRepository;
import com.revplay.musicplatform.premium.repository.UserSubscriptionRepository;
import com.revplay.musicplatform.systemplaylist.repository.SystemPlaylistRepository;
import com.revplay.musicplatform.user.repository.UserRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminBusinessAnalyticsServiceImpl implements AdminBusinessAnalyticsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBusinessAnalyticsServiceImpl.class);

    private static final String COUNT_ACTIVE_PREMIUM_SQL = """
            SELECT COUNT(DISTINCT us.user_id)
            FROM user_subscriptions us
            WHERE us.status = 'ACTIVE' AND us.end_date > ?
            """;

    private static final String SUM_REVENUE_IN_RANGE_SQL = """
            SELECT COALESCE(SUM(sp.amount), 0)
            FROM subscription_payments sp
            WHERE sp.payment_status = 'SUCCESS' AND sp.paid_at >= ? AND sp.paid_at < ?
            """;

    private static final String SUM_TOTAL_REVENUE_SQL = """
            SELECT COALESCE(SUM(sp.amount), 0)
            FROM subscription_payments sp
            WHERE sp.payment_status = 'SUCCESS'
            """;

    private static final String TOP_DOWNLOADS_SQL = """
            SELECT sd.song_id, COUNT(*) AS download_count
            FROM song_downloads sd
            JOIN songs s ON s.song_id = sd.song_id
            WHERE s.is_active = true
            GROUP BY sd.song_id
            ORDER BY download_count DESC, sd.song_id ASC
            LIMIT ?
            """;

    private static final String TOP_MIXES_SQL = """
            SELECT sp.name AS playlist_name, COUNT(ph.play_id) AS total_play_count
            FROM system_playlists sp
            LEFT JOIN system_playlist_songs sps
                   ON sps.system_playlist_id = sp.id
                  AND (sps.deleted_at IS NULL)
            LEFT JOIN play_history ph
                   ON ph.song_id = sps.song_id
            WHERE sp.deleted_at IS NULL
            GROUP BY sp.id, sp.name
            ORDER BY total_play_count DESC, sp.name ASC
            """;

    private final UserRepository userRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionPaymentRepository subscriptionPaymentRepository;
    private final AdImpressionRepository adImpressionRepository;
    private final SongDownloadRepository songDownloadRepository;
    private final PlayHistoryRepository playHistoryRepository;
    private final SystemPlaylistRepository systemPlaylistRepository;
    private final JdbcTemplate jdbcTemplate;

    public AdminBusinessAnalyticsServiceImpl(
            UserRepository userRepository,
            UserSubscriptionRepository userSubscriptionRepository,
            SubscriptionPaymentRepository subscriptionPaymentRepository,
            AdImpressionRepository adImpressionRepository,
            SongDownloadRepository songDownloadRepository,
            PlayHistoryRepository playHistoryRepository,
            SystemPlaylistRepository systemPlaylistRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.userRepository = userRepository;
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.subscriptionPaymentRepository = subscriptionPaymentRepository;
        this.adImpressionRepository = adImpressionRepository;
        this.songDownloadRepository = songDownloadRepository;
        this.playHistoryRepository = playHistoryRepository;
        this.systemPlaylistRepository = systemPlaylistRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessOverviewResponse getBusinessOverview() {
        long totalUsers = userRepository.count();
        long totalAdImpressions = adImpressionRepository.count();
        long totalDownloads = songDownloadRepository.count();
        long totalSongPlays = playHistoryRepository.count();
        long activePremiumUsers = getActivePremiumUsersCount();

        LOGGER.info("Business overview computed: users={}, premiumUsers={}, adImpressions={}, downloads={}, plays={}",
                totalUsers, activePremiumUsers, totalAdImpressions, totalDownloads, totalSongPlays);

        return new BusinessOverviewResponse(
                totalUsers,
                activePremiumUsers,
                totalAdImpressions,
                totalDownloads,
                totalSongPlays
        );
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueAnalyticsResponse getRevenueAnalytics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime monthEnd = monthStart.plusMonths(1);

        LocalDateTime yearStart = now.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime yearEnd = yearStart.plusYears(1);

        double monthlyRevenue = queryRevenueInRange(monthStart, monthEnd);
        double yearlyRevenue = queryRevenueInRange(yearStart, yearEnd);
        double totalRevenue = queryTotalRevenue();

        LOGGER.info("Revenue analytics computed: monthly={}, yearly={}, total={}", monthlyRevenue, yearlyRevenue, totalRevenue);
        return new RevenueAnalyticsResponse(monthlyRevenue, yearlyRevenue, totalRevenue);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopDownloadResponse> getTopDownloadedSongs(int limit) {
        int safeLimit = limit <= 0 ? 10 : Math.min(limit, 100);
        return jdbcTemplate.query(
                TOP_DOWNLOADS_SQL,
                (rs, rowNum) -> new TopDownloadResponse(
                        rs.getLong("song_id"),
                        rs.getLong("download_count")
                ),
                safeLimit
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopMixResponse> getTopMixes() {
        return jdbcTemplate.query(
                TOP_MIXES_SQL,
                this::mapTopMix
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ConversionRateResponse getPremiumConversionRate() {
        long totalUsers = userRepository.count();
        long activePremiumUsers = getActivePremiumUsersCount();
        double percentage = totalUsers == 0 ? 0.0 : (activePremiumUsers * 100.0) / totalUsers;
        return new ConversionRateResponse(roundToTwoDecimals(percentage));
    }

    private long getActivePremiumUsersCount() {
        LocalDateTime now = LocalDateTime.now();
        Long count = jdbcTemplate.queryForObject(COUNT_ACTIVE_PREMIUM_SQL, Long.class, now);
        return count == null ? 0L : count;
    }

    private double queryRevenueInRange(LocalDateTime from, LocalDateTime to) {
        Double value = jdbcTemplate.queryForObject(SUM_REVENUE_IN_RANGE_SQL, Double.class, from, to);
        return value == null ? 0.0 : value;
    }

    private double queryTotalRevenue() {
        Double value = jdbcTemplate.queryForObject(SUM_TOTAL_REVENUE_SQL, Double.class);
        return value == null ? 0.0 : value;
    }

    private TopMixResponse mapTopMix(ResultSet rs, int rowNum) throws SQLException {
        return new TopMixResponse(
                rs.getString("playlist_name"),
                rs.getLong("total_play_count")
        );
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
