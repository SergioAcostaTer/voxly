package com.pigs.voxly.application.identity.eventhandlers;

import com.pigs.voxly.application.identity.ports.EmailService;
import com.pigs.voxly.domain.identity.events.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class UserRegisteredEventHandler {

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredEventHandler.class);

    private final EmailService emailService;

    public UserRegisteredEventHandler(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    @EventListener
    public void handle(UserRegisteredEvent event) {
        try {
            emailService.sendEmailVerification(
                    event.getEmail(),
                    event.getUsername(),
                    event.getEmailVerificationToken());
        } catch (Exception e) {
            log.error("Failed to send email verification to {}: {}", event.getEmail(), e.getMessage(), e);
        }
    }
}
