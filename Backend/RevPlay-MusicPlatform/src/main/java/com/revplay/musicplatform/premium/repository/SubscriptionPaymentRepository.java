package com.revplay.musicplatform.premium.repository;

import com.revplay.musicplatform.premium.entity.SubscriptionPayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, Long> {
}

