package com.pigs.voxly.application.identity.eventhandlers;

import com.pigs.voxly.application.identity.ports.EmailService;
import com.pigs.voxly.domain.identity.events.TwoFactorCodeGeneratedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class TwoFactorCodeGeneratedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(TwoFactorCodeGeneratedEventHandler.class);

    private final EmailService emailService;

    public TwoFactorCodeGeneratedEventHandler(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    @EventListener
    public void handle(TwoFactorCodeGeneratedEvent event) {
        try {
            emailService.sendTwoFactorCode(
                    event.getEmail(),
                    event.getUsername(),
                    event.getCode());
        } catch (Exception e) {
            log.error("Failed to send 2FA code to {}: {}", event.getEmail(), e.getMessage(), e);
        }
    }
}
