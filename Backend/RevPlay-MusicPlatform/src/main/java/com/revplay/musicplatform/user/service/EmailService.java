package com.revplay.musicplatform.user.service;

public interface EmailService {

    void sendEmail(String toEmail, String subject, String body);

    void sendWelcomeEmail(String toEmail, String userName);

    void sendPremiumSubscriptionEmail(String toEmail, String userName, String planType);
}
