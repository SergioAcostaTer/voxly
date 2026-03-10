package com.pigs.voxly.application.identity.eventhandlers;

import com.pigs.voxly.application.identity.ports.EmailService;
import com.pigs.voxly.domain.identity.events.PasswordResetRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetRequestedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetRequestedEventHandler.class);

    private final EmailService emailService;

    public PasswordResetRequestedEventHandler(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    @EventListener
    public void handle(PasswordResetRequestedEvent event) {
        try {
            emailService.sendPasswordReset(
                    event.getEmail(),
                    event.getUsername(),
                    event.getResetToken());
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", event.getEmail(), e.getMessage(), e);
        }
    }
}
