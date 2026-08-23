package com.autosalone.schedulers;

import com.autosalone.services.AuthTokenService;

import jakarta.enterprise.concurrent.Asynchronous;
import jakarta.enterprise.concurrent.Schedule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AuthTokenExpirationScheduler {

    @Inject
    private AuthTokenService authTokenService;

    // Esecuzione: Ogni Domenica alle 03:00:00
    @Asynchronous(runAt = @Schedule(cron = "0 0 3 * * SUN"))
    public void deleteExpiredAuthTokens() {
        System.out.println("Avvio job di background (Concurrency API): Pulizia token scaduti...");

        authTokenService.deleteExpiredAuthTokens();

        System.out.println("Job pulizia token completato");
    }
}