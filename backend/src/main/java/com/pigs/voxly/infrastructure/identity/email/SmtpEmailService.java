package com.pigs.voxly.infrastructure.identity.email;

import com.pigs.voxly.application.identity.ports.EmailService;
import com.pigs.voxly.infrastructure.identity.config.AppMailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final AppMailProperties mailProperties;

    public SmtpEmailService(JavaMailSender mailSender, AppMailProperties mailProperties) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
    }

    @Override
    public void sendEmailVerification(String to, String username, String verificationToken) {
        String verificationUrl = mailProperties.baseUrl() + "/v1/auth/verify-email?token=" + verificationToken;

        String body = """
                <h2>Welcome to VoxLy, %s!</h2>
                <p>Please verify your email address by clicking the link below:</p>
                <p><a href="%s">Verify Email</a></p>
                <p>This link will expire in 24 hours.</p>
                <p>If you didn't create an account, please ignore this email.</p>
                """.formatted(username, verificationUrl);

        sendHtmlEmail(to, "Verify your VoxLy email", body);
    }

    @Override
    public void sendPasswordReset(String to, String username, String resetToken) {
        String resetUrl = mailProperties.baseUrl() + "/v1/auth/reset-password?token=" + resetToken;

        String body = """
                <h2>Password Reset Request</h2>
                <p>Hi %s, we received a request to reset your password.</p>
                <p><a href="%s">Reset Password</a></p>
                <p>This link will expire in 1 hour.</p>
                <p>If you didn't request a password reset, please ignore this email.</p>
                """.formatted(username, resetUrl);

        sendHtmlEmail(to, "Reset your VoxLy password", body);
    }

    @Override
    public void sendTwoFactorCode(String to, String username, String code) {
        String body = """
                <h2>Your Two-Factor Authentication Code</h2>
                <p>Hi %s, your verification code is:</p>
                <h1 style="letter-spacing: 8px; font-size: 36px;">%s</h1>
                <p>This code will expire in 10 minutes.</p>
                <p>If you didn't request this code, please secure your account immediately.</p>
                """.formatted(username, code);

        sendHtmlEmail(to, "VoxLy - Your verification code", body);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailProperties.fromEmail(), mailProperties.fromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
