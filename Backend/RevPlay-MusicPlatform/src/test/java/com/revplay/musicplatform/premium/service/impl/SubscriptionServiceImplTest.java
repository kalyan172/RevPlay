package com.revplay.musicplatform.premium.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.exception.BadRequestException;
import com.revplay.musicplatform.premium.dto.PremiumStatusResponse;
import com.revplay.musicplatform.premium.entity.SubscriptionPayment;
import com.revplay.musicplatform.premium.entity.UserSubscription;
import com.revplay.musicplatform.premium.enums.PaymentStatus;
import com.revplay.musicplatform.premium.enums.PlanType;
import com.revplay.musicplatform.premium.enums.SubscriptionStatus;
import com.revplay.musicplatform.premium.repository.SubscriptionPaymentRepository;
import com.revplay.musicplatform.premium.repository.UserSubscriptionRepository;
import com.revplay.musicplatform.user.entity.User;
import com.revplay.musicplatform.user.repository.UserRepository;
import com.revplay.musicplatform.user.service.EmailService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class SubscriptionServiceImplTest {

    private static final Long USER_ID = 10L;
    private static final Long SUBSCRIPTION_ID = 55L;
    private static final String MONTHLY = "MONTHLY";
    private static final String YEARLY = "YEARLY";
    private static final String EMAIL = "premium@example.com";
    private static final String USERNAME = "premium-user";
    private static final String USER_ID_REQUIRED_MESSAGE = "userId is required";
    private static final String PLAN_REQUIRED_MESSAGE = "planType is required";
    private static final String INVALID_PLAN_MESSAGE = "Unsupported planType. Use MONTHLY or YEARLY";
    private static final double MONTHLY_AMOUNT = 199.0;
    private static final double YEARLY_AMOUNT = 1499.0;
    private static final long MONTHLY_DAYS = 30L;
    private static final long YEARLY_DAYS = 365L;
    private static final String INR = "INR";
    private static final String PAYMENT_METHOD = "DUMMY";

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;
    @Mock
    private SubscriptionPaymentRepository subscriptionPaymentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private SubscriptionServiceImpl service;

    @Test
    @DisplayName("isUserPremium returns false when no active subscription exists")
    void isUserPremiumReturnsFalseWhenNoActiveSubscription() {
        when(userSubscriptionRepository.findFirstByUserIdAndStatusOrderByEndDateDesc(USER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        boolean result = service.isUserPremium(USER_ID);

        assertThat(result).isFalse();
        verify(userSubscriptionRepository).findFirstByUserIdAndStatusOrderByEndDateDesc(USER_ID, SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("isUserPremium returns true when active subscription end date is in future")
    void isUserPremiumReturnsTrueWhenActiveSubscriptionNotExpired() {
        UserSubscription active = subscription(PlanType.MONTHLY, LocalDateTime.now().plusDays(5), SubscriptionStatus.ACTIVE);
        when(userSubscriptionRepository.findFirstByUserIdAndStatusOrderByEndDateDesc(USER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(active));

        boolean result = service.isUserPremium(USER_ID);

        assertThat(result).isTrue();
        verify(userSubscriptionRepository, never()).save(any(UserSubscription.class));
    }

    @Test
    @DisplayName("isUserPremium expires outdated active subscription and returns false")
    void isUserPremiumExpiresOutdatedSubscription() {
        UserSubscription active = subscription(PlanType.MONTHLY, LocalDateTime.now().minusMinutes(1), SubscriptionStatus.ACTIVE);
        when(userSubscriptionRepository.findFirstByUserIdAndStatusOrderByEndDateDesc(USER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(active));

        boolean result = service.isUserPremium(USER_ID);

        assertThat(result).isFalse();
        assertThat(active.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        verify(userSubscriptionRepository).save(active);
    }

    @Test
    @DisplayName("isUserPremium throws bad request when user id is null")
    void isUserPremiumThrowsWhenUserIdNull() {
        assertThatThrownBy(() -> service.isUserPremium(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(USER_ID_REQUIRED_MESSAGE);
    }

    @Test
    @DisplayName("upgradeToPremium creates new active monthly subscription and payment")
    void upgradeToPremiumCreatesNewSubscriptionAndPayment() {
        LocalDateTime before = LocalDateTime.now();
        when(userSubscriptionRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE)).thenReturn(List.of());
        when(userSubscriptionRepository.save(any(UserSubscription.class))).thenAnswer(invocation -> {
            UserSubscription saved = invocation.getArgument(0);
            saved.setId(SUBSCRIPTION_ID);
            return saved;
        });
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));

        service.upgradeToPremium(USER_ID, MONTHLY);

        ArgumentCaptor<UserSubscription> subscriptionCaptor = ArgumentCaptor.forClass(UserSubscription.class);
        verify(userSubscriptionRepository).save(subscriptionCaptor.capture());
        UserSubscription savedSubscription = subscriptionCaptor.getValue();
        assertThat(savedSubscription.getPlanType()).isEqualTo(PlanType.MONTHLY);
        assertThat(savedSubscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(savedSubscription.getEndDate()).isAfter(savedSubscription.getStartDate());
        assertThat(savedSubscription.getEndDate()).isAfter(before.plusDays(MONTHLY_DAYS - 1));

        ArgumentCaptor<SubscriptionPayment> paymentCaptor = ArgumentCaptor.forClass(SubscriptionPayment.class);
        verify(subscriptionPaymentRepository).save(paymentCaptor.capture());
        SubscriptionPayment payment = paymentCaptor.getValue();
        assertThat(payment.getSubscriptionId()).isEqualTo(SUBSCRIPTION_ID);
        assertThat(payment.getAmount()).isEqualTo(MONTHLY_AMOUNT);
        assertThat(payment.getCurrency()).isEqualTo(INR);
        assertThat(payment.getPaymentMethod()).isEqualTo(PAYMENT_METHOD);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getTransactionReference()).startsWith("DUMMY-");

        verify(emailService).sendPremiumSubscriptionEmail(EMAIL, USERNAME, MONTHLY);
    }

    @Test
    @DisplayName("upgradeToPremium extends existing active subscription and cancels extras")
    void upgradeToPremiumExtendsExistingAndCancelsExtraActiveSubscriptions() {
        UserSubscription primary = subscription(PlanType.MONTHLY, LocalDateTime.now().plusDays(5), SubscriptionStatus.ACTIVE);
        primary.setId(SUBSCRIPTION_ID);
        UserSubscription extra = subscription(PlanType.MONTHLY, LocalDateTime.now().plusDays(3), SubscriptionStatus.ACTIVE);
        extra.setId(99L);
        when(userSubscriptionRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(primary, extra));
        when(userSubscriptionRepository.save(any(UserSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        LocalDateTime previousEnd = primary.getEndDate();
        service.upgradeToPremium(USER_ID, YEARLY);

        assertThat(primary.getPlanType()).isEqualTo(PlanType.YEARLY);
        assertThat(primary.getEndDate()).isAfter(previousEnd.plusDays(YEARLY_DAYS - 1));
        assertThat(primary.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(extra.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        verify(subscriptionPaymentRepository).save(any(SubscriptionPayment.class));
        verify(emailService, never()).sendPremiumSubscriptionEmail(any(), any(), any());
    }

    @Test
    @DisplayName("upgradeToPremium throws bad request when user id invalid")
    void upgradeToPremiumThrowsWhenUserIdInvalid() {
        assertThatThrownBy(() -> service.upgradeToPremium(0L, MONTHLY))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(USER_ID_REQUIRED_MESSAGE);
    }

    @Test
    @DisplayName("upgradeToPremium throws bad request when plan type is blank")
    void upgradeToPremiumThrowsWhenPlanTypeBlank() {
        assertThatThrownBy(() -> service.upgradeToPremium(USER_ID, " "))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(PLAN_REQUIRED_MESSAGE);
    }

    @Test
    @DisplayName("upgradeToPremium throws bad request when plan type is unsupported")
    void upgradeToPremiumThrowsWhenPlanTypeUnsupported() {
        assertThatThrownBy(() -> service.upgradeToPremium(USER_ID, "weekly"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(INVALID_PLAN_MESSAGE);
    }

    @Test
    @DisplayName("upgradeToPremium swallows email exception and still persists payment")
    void upgradeToPremiumSwallowsEmailFailure() {
        when(userSubscriptionRepository.findByUserIdAndStatus(USER_ID, SubscriptionStatus.ACTIVE)).thenReturn(List.of());
        when(userSubscriptionRepository.save(any(UserSubscription.class))).thenAnswer(invocation -> {
            UserSubscription saved = invocation.getArgument(0);
            saved.setId(SUBSCRIPTION_ID);
            return saved;
        });
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        org.mockito.Mockito.doThrow(new RuntimeException("mail down"))
                .when(emailService).sendPremiumSubscriptionEmail(eq(EMAIL), eq(USERNAME), eq(MONTHLY));

        service.upgradeToPremium(USER_ID, MONTHLY);

        verify(subscriptionPaymentRepository).save(any(SubscriptionPayment.class));
        verify(emailService).sendPremiumSubscriptionEmail(EMAIL, USERNAME, MONTHLY);
    }

    @Test
    @DisplayName("getPremiumStatus returns premium true and expiry date when active")
    void getPremiumStatusReturnsPremiumWithExpiry() {
        LocalDateTime expiry = LocalDateTime.now().plusDays(1);
        UserSubscription active = subscription(PlanType.MONTHLY, expiry, SubscriptionStatus.ACTIVE);
        when(userSubscriptionRepository.findFirstByUserIdAndStatusOrderByEndDateDesc(USER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(active), Optional.of(active));

        PremiumStatusResponse response = service.getPremiumStatus(USER_ID);

        assertThat(response.isPremium()).isTrue();
        assertThat(response.expiryDate()).isEqualTo(expiry);
    }

    @Test
    @DisplayName("getPremiumStatus returns premium false with null expiry when inactive")
    void getPremiumStatusReturnsNonPremiumWithNullExpiry() {
        when(userSubscriptionRepository.findFirstByUserIdAndStatusOrderByEndDateDesc(USER_ID, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        PremiumStatusResponse response = service.getPremiumStatus(USER_ID);

        assertThat(response.isPremium()).isFalse();
        assertThat(response.expiryDate()).isNull();
    }

    private UserSubscription subscription(PlanType planType, LocalDateTime endDate, SubscriptionStatus status) {
        UserSubscription subscription = new UserSubscription();
        subscription.setUserId(USER_ID);
        subscription.setPlanType(planType);
        subscription.setStartDate(LocalDateTime.now().minusDays(1));
        subscription.setEndDate(endDate);
        subscription.setStatus(status);
        return subscription;
    }

    private User user() {
        User user = new User();
        user.setUserId(USER_ID);
        user.setEmail(EMAIL);
        user.setUsername(USERNAME);
        return user;
    }
}
