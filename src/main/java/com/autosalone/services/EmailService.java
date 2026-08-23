package com.autosalone.services;

public interface EmailService {
    void sendRegistrationInvite(String toEmail, String token);

    void sendPasswordReset(String toEmail, String token);
}