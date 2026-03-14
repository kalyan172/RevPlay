package com.revplay.musicplatform.premium.service.impl;

import com.revplay.musicplatform.exception.BadRequestException;
import com.revplay.musicplatform.premium.dto.PremiumStatusResponse;
import com.revplay.musicplatform.premium.entity.SubscriptionPayment;
import com.revplay.musicplatform.premium.entity.UserSubscription;
import com.revplay.musicplatform.premium.enums.PaymentStatus;
import com.revplay.musicplatform.premium.enums.PlanType;
import com.revplay.musicplatform.premium.enums.SubscriptionStatus;
import com.revplay.musicplatform.premium.repository.SubscriptionPaymentRepository;
import com.revplay.musicplatform.premium.repository.UserSubscriptionRepository;
import com.revplay.musicplatform.premium.service.SubscriptionService;
import com.revplay.musicplatform.user.repository.UserRepository;
import com.revplay.musicplatform.user.service.EmailService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionServiceImpl.class);
    private static final double MONTHLY_AMOUNT = 199.0;
    private static final double YEARLY_AMOUNT = 1499.0;

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionPaymentRepository subscriptionPaymentRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public SubscriptionServiceImpl(
            UserSubscriptionRepository userSubscriptionRepository,
            SubscriptionPaymentRepository subscriptionPaymentRepository,
            UserRepository userRepository,
            EmailService emailService
    ) {
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.subscriptionPaymentRepository = subscriptionPaymentRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public boolean isUserPremium(Long userId) {
        validateUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        UserSubscription active = userSubscriptionRepository
                .findFirstByUserIdAndStatusOrderByEndDateDesc(userId, SubscriptionStatus.ACTIVE)
                .orElse(null);

        if (active == null) {
            return false;
        }
        if (active.getEndDate().isAfter(now)) {
            return true;
        }

        active.setStatus(SubscriptionStatus.EXPIRED);
        userSubscriptionRepository.save(active);
        LOGGER.info("Subscription auto-expired for userId={}, subscriptionId={}", userId, active.getId());
        return false;
    }

    @Override
    @Transactional
    public void upgradeToPremium(Long userId, String planType) {
        validateUserId(userId);
        PlanType parsedPlanType = parsePlanType(planType);
        LocalDateTime now = LocalDateTime.now();

        List<UserSubscription> activeSubscriptions = userSubscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);
        UserSubscription targetSubscription;

        if (activeSubscriptions.isEmpty()) {
            targetSubscription = new UserSubscription();
            targetSubscription.setUserId(userId);
            targetSubscription.setPlanType(parsedPlanType);
            targetSubscription.setStartDate(now);
            targetSubscription.setEndDate(now.plusDays(daysForPlan(parsedPlanType)));
            targetSubscription.setStatus(SubscriptionStatus.ACTIVE);
            targetSubscription = userSubscriptionRepository.save(targetSubscription);
            LOGGER.info("Created new premium subscription for userId={}, subscriptionId={}", userId, targetSubscription.getId());
        } else {
            targetSubscription = activeSubscriptions.get(0);
            for (int i = 1; i < activeSubscriptions.size(); i++) {
                UserSubscription extra = activeSubscriptions.get(i);
                extra.setStatus(SubscriptionStatus.CANCELLED);
                userSubscriptionRepository.save(extra);
                LOGGER.warn("Cancelled extra ACTIVE subscription for userId={}, subscriptionId={}", userId, extra.getId());
            }

            targetSubscription.setPlanType(parsedPlanType);
            LocalDateTime baseDate = targetSubscription.getEndDate().isAfter(now) ? targetSubscription.getEndDate() : now;
            if (!targetSubscription.getEndDate().isAfter(now)) {
                targetSubscription.setStartDate(now);
            }
            targetSubscription.setEndDate(baseDate.plusDays(daysForPlan(parsedPlanType)));
            targetSubscription.setStatus(SubscriptionStatus.ACTIVE);
            targetSubscription = userSubscriptionRepository.save(targetSubscription);
            LOGGER.info("Extended premium subscription for userId={}, subscriptionId={}", userId, targetSubscription.getId());
        }

        SubscriptionPayment payment = new SubscriptionPayment();
        payment.setUserId(userId);
        payment.setSubscriptionId(targetSubscription.getId());
        payment.setAmount(amountForPlan(parsedPlanType));
        payment.setCurrency("INR");
        payment.setPaymentMethod("DUMMY");
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setTransactionReference("DUMMY-" + UUID.randomUUID());
        payment.setPaidAt(now);
        subscriptionPaymentRepository.save(payment);
        LOGGER.info("Dummy subscription payment stored for userId={}, subscriptionId={}", userId, targetSubscription.getId());

        try {
            userRepository.findById(userId).ifPresent(user ->
                    emailService.sendPremiumSubscriptionEmail(
                            user.getEmail(),
                            user.getUsername(),
                            parsedPlanType.name()
                    )
            );
        } catch (Exception ex) {
            LOGGER.error("Premium subscription email send failed for userId={}", userId, ex);
        }
    }

    @Override
    @Transactional
    public PremiumStatusResponse getPremiumStatus(Long userId) {
        boolean premium = isUserPremium(userId);
        LocalDateTime expiryDate = null;
        if (premium) {
            expiryDate = userSubscriptionRepository
                    .findFirstByUserIdAndStatusOrderByEndDateDesc(userId, SubscriptionStatus.ACTIVE)
                    .map(UserSubscription::getEndDate)
                    .orElse(null);
        }
        return new PremiumStatusResponse(premium, expiryDate);
    }

    private PlanType parsePlanType(String planType) {
        if (planType == null || planType.isBlank()) {
            throw new BadRequestException("planType is required");
        }
        try {
            return PlanType.valueOf(planType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported planType. Use MONTHLY or YEARLY");
        }
    }

    private long daysForPlan(PlanType planType) {
        return switch (planType) {
            case MONTHLY -> 30L;
            case YEARLY -> 365L;
        };
    }

    private double amountForPlan(PlanType planType) {
        return switch (planType) {
            case MONTHLY -> MONTHLY_AMOUNT;
            case YEARLY -> YEARLY_AMOUNT;
        };
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BadRequestException("userId is required");
        }
    }
}
