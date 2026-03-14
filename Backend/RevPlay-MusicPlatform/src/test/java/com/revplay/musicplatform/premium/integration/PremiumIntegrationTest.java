package com.revplay.musicplatform.premium.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.revplay.musicplatform.catalog.service.DiscoveryPerformanceService;
import com.revplay.musicplatform.premium.entity.UserSubscription;
import com.revplay.musicplatform.premium.enums.PlanType;
import com.revplay.musicplatform.premium.enums.SubscriptionStatus;
import com.revplay.musicplatform.premium.repository.SubscriptionPaymentRepository;
import com.revplay.musicplatform.premium.repository.UserSubscriptionRepository;
import com.revplay.musicplatform.premium.service.SubscriptionService;
import com.revplay.musicplatform.user.entity.User;
import com.revplay.musicplatform.user.enums.UserRole;
import com.revplay.musicplatform.user.repository.UserRepository;
import com.revplay.musicplatform.user.service.EmailService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class PremiumIntegrationTest {

    private static final String MONTHLY = "MONTHLY";

    private final SubscriptionService subscriptionService;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionPaymentRepository subscriptionPaymentRepository;
    private final UserRepository userRepository;

    @MockBean
    private EmailService emailService;
    @MockBean
    private DiscoveryPerformanceService discoveryPerformanceService;

    private Long userId;

    @Autowired
    PremiumIntegrationTest(
            SubscriptionService subscriptionService,
            UserSubscriptionRepository userSubscriptionRepository,
            SubscriptionPaymentRepository subscriptionPaymentRepository,
            UserRepository userRepository
    ) {
        this.subscriptionService = subscriptionService;
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.subscriptionPaymentRepository = subscriptionPaymentRepository;
        this.userRepository = userRepository;
    }

    @BeforeEach
    void setUp() {
        subscriptionPaymentRepository.deleteAll();
        userSubscriptionRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("premium-int@example.com");
        user.setUsername("premium-int-user");
        user.setPasswordHash("hash");
        user.setRole(UserRole.LISTENER);
        user.setIsActive(Boolean.TRUE);
        user.setEmailVerified(Boolean.TRUE);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userId = userRepository.save(user).getUserId();
    }

    @Test
    @DisplayName("upgrade persists active subscription and payment")
    void upgradePersistsSubscriptionAndPayment() {
        subscriptionService.upgradeToPremium(userId, MONTHLY);

        List<UserSubscription> active = userSubscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);
        assertThat(active).hasSize(1);
        assertThat(active.get(0).getPlanType()).isEqualTo(PlanType.MONTHLY);
        assertThat(subscriptionPaymentRepository.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("isUserPremium returns true after upgrade")
    void isUserPremiumReturnsTrueAfterUpgrade() {
        subscriptionService.upgradeToPremium(userId, MONTHLY);

        boolean premium = subscriptionService.isUserPremium(userId);

        assertThat(premium).isTrue();
    }

    @Test
    @DisplayName("expired active subscription is marked expired by isUserPremium")
    void expiredSubscriptionGetsMarkedExpired() {
        UserSubscription subscription = new UserSubscription();
        subscription.setUserId(userId);
        subscription.setPlanType(PlanType.MONTHLY);
        subscription.setStartDate(LocalDateTime.now().minusDays(2));
        subscription.setEndDate(LocalDateTime.now().minusMinutes(1));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        userSubscriptionRepository.save(subscription);

        boolean premium = subscriptionService.isUserPremium(userId);

        assertThat(premium).isFalse();
        UserSubscription updated = userSubscriptionRepository
                .findFirstByUserIdAndStatusOrderByEndDateDesc(userId, SubscriptionStatus.EXPIRED)
                .orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
    }
}
