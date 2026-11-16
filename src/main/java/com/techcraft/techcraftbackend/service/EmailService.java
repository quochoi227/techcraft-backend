package com.techcraft.techcraftbackend.service;

public interface EmailService {

    void sendVerificationEmail(String toEmail, String recipientName, String token);

    void sendPasswordResetEmail(String toEmail, String recipientName, String token);
}
