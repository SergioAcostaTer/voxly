package com.pigs.voxly.application.identity.eventhandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.pigs.voxly.application.identity.ports.EmailService;
import com.pigs.voxly.application.identity.ports.IdentitySettingsRepository;
import com.pigs.voxly.domain.identity.events.UserRegisteredEvent;

@Component
public class UserRegisteredEventHandler {

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredEventHandler.class);

    private final EmailService emailService;
    private final IdentitySettingsRepository identitySettingsRepository;

    public UserRegisteredEventHandler(EmailService emailService,
            IdentitySettingsRepository identitySettingsRepository) {
        this.emailService = emailService;
        this.identitySettingsRepository = identitySettingsRepository;
    }

    @Async
    @EventListener
    public void handle(UserRegisteredEvent event) {
        if (!identitySettingsRepository.isEmailVerificationRequired()) {
            return;
        }

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
