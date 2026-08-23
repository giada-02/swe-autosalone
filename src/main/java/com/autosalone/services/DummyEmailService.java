package com.autosalone.services;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DummyEmailService implements EmailService {

    private static final String FRONTEND_URL = "http://localhost:3000";

    @Override
    public void sendRegistrationInvite(String toEmail, String token) {
        String link = FRONTEND_URL + "/signup?token=" + token;

        System.out.println("==============================================");
        System.out.println("INVIO EMAIL DI INVITO DI REGISTRAZIONE");
        System.out.println("A: " + toEmail);
        System.out.println("Link: " + link);
        System.out.println("==============================================");
    }

    @Override
    public void sendPasswordReset(String toEmail, String token) {
        String link = FRONTEND_URL + "/reset-password?token=" + token;

        System.out.println("==============================================");
        System.out.println("INVIO EMAIL RECUPERO PASSWORD");
        System.out.println("A: " + toEmail);
        System.out.println("Link: " + link);
        System.out.println("==============================================");
    }
}