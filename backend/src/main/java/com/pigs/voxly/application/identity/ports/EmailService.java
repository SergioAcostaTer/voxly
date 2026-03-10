package com.pigs.voxly.application.identity.ports;

public interface EmailService {

    void sendEmailVerification(String to, String username, String verificationToken);

    void sendPasswordReset(String to, String username, String resetToken);

    void sendTwoFactorCode(String to, String username, String code);
}
