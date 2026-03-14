package com.revplay.musicplatform.user.service.impl;

import com.revplay.musicplatform.user.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailServiceImpl.class);
    private static final String SENDER_FROM = "RevPlay <balusharshasri2807@gmail.com>";

    private final JavaMailSender javaMailSender;
    private final String mailFrom;

    public EmailServiceImpl(
            JavaMailSender javaMailSender,
            @Value("${spring.mail.username:}") String mailFrom
    ) {
        this.javaMailSender = javaMailSender;
        this.mailFrom = mailFrom;
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String userName) {
        String safeName = normalizeName(userName);
        String body = """
                Hi %s,

                Welcome to RevPlay.

                Your account is ready and you can now start listening.

                RevPlay brings together music you love and music you are about to discover.

                On RevPlay you can:

                - Discover trending songs
                - Explore curated Mix Playlists
                - Stream music from your favorite artists
                - Save songs and build your personal music collection

                Thousands of tracks are waiting for you.

                Open RevPlay and start listening.

                RevPlay
                Music without interruptions
                """.formatted(safeName);
        sendEmail(toEmail, "Welcome to RevPlay - Your music starts here", body);
    }

    @Override
    public void sendPremiumSubscriptionEmail(String toEmail, String userName, String planType) {
        String safeName = normalizeName(userName);
        String safePlan = (planType == null || planType.isBlank()) ? "UNKNOWN" : planType.trim().toUpperCase();
        String body = """
                Hi %s,

                Your RevPlay Premium subscription has been successfully activated.

                Premium unlocks the full RevPlay experience.

                With Premium you can now enjoy:

                - Ad-free music playback
                - Download songs for offline listening
                - Unlimited uninterrupted streaming
                - Faster and smoother playback

                Plan: %s

                Everything you love about music - without interruptions.

                Enjoy the music.

                RevPlay
                """.formatted(safeName, safePlan);
        sendEmail(toEmail, "Your RevPlay Premium is now active", body);
    }

    @Override
    public void sendEmail(String toEmail, String subject, String body) {
        if (!StringUtils.hasText(mailFrom)) {
            throw new IllegalStateException("Email service is not configured. Set MAIL_USERNAME.");
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(SENDER_FROM);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, false);
            javaMailSender.send(message);
        } catch (MessagingException ex) {
            LOGGER.error("Failed to compose email to {} with subject '{}': {}", toEmail, subject, ex.getMessage(), ex);
            throw new IllegalStateException("Unable to compose email", ex);
        } catch (MailException ex) {
            LOGGER.error("Failed to send email to {} with subject '{}': {}", toEmail, subject, ex.getMessage(), ex);
            throw ex;
        }
    }

    private String normalizeName(String userName) {
        return (userName == null || userName.isBlank()) ? "User" : userName.trim();
    }
}
