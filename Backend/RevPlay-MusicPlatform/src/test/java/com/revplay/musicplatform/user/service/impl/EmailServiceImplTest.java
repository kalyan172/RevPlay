package com.revplay.musicplatform.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class EmailServiceImplTest {

    private static final String MAIL_FROM = "noreply@revplay.com";
    private static final String TO_EMAIL = "user@example.com";
    private static final String SUBJECT = "subject";
    private static final String BODY = "body";
    private static final String USERNAME = "Jay";
    private static final String BLANK = " ";

    @Mock
    private JavaMailSender javaMailSender;

    @Test
    @DisplayName("sendEmail throws when mail username not configured")
    void sendEmailThrowsWhenMailNotConfigured() {
        EmailServiceImpl service = new EmailServiceImpl(javaMailSender, "");

        assertThatThrownBy(() -> service.sendEmail(TO_EMAIL, SUBJECT, BODY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Email service is not configured. Set MAIL_USERNAME.");
    }

    @Test
    @DisplayName("sendEmail composes and sends mime message")
    void sendEmailComposesAndSendsMimeMessage() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        EmailServiceImpl service = new EmailServiceImpl(javaMailSender, MAIL_FROM);

        service.sendEmail(TO_EMAIL, SUBJECT, BODY);

        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendEmail propagates mail exception")
    void sendEmailPropagatesMailException() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("mail-down")).when(javaMailSender).send(mimeMessage);
        EmailServiceImpl service = new EmailServiceImpl(javaMailSender, MAIL_FROM);

        assertThatThrownBy(() -> service.sendEmail(TO_EMAIL, SUBJECT, BODY))
                .isInstanceOf(MailSendException.class);
    }

    @Test
    @DisplayName("sendWelcomeEmail normalizes blank username to user")
    void sendWelcomeEmailNormalizesBlankUserName() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        EmailServiceImpl service = new EmailServiceImpl(javaMailSender, MAIL_FROM);

        service.sendWelcomeEmail(TO_EMAIL, BLANK);

        assertThat(mimeMessage.getSubject()).isEqualTo("Welcome to RevPlay - Your music starts here");
        Object content = mimeMessage.getContent();
        assertThat(content.toString()).contains("Hi User");
    }

    @Test
    @DisplayName("sendPremiumSubscriptionEmail uses UNKNOWN for blank plan")
    void sendPremiumSubscriptionEmailUsesUnknownPlanWhenBlank() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        EmailServiceImpl service = new EmailServiceImpl(javaMailSender, MAIL_FROM);

        service.sendPremiumSubscriptionEmail(TO_EMAIL, USERNAME, BLANK);

        assertThat(mimeMessage.getSubject()).isEqualTo("Your RevPlay Premium is now active");
        Object content = mimeMessage.getContent();
        assertThat(content.toString()).contains("Plan: UNKNOWN");
    }
}
