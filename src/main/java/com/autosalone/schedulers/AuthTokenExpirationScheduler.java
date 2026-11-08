package com.autosalone.schedulers;

import com.autosalone.services.AuthTokenService;

import jakarta.enterprise.concurrent.Asynchronous;
import jakarta.enterprise.concurrent.Schedule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;

@ApplicationScoped
public class AuthTokenExpirationScheduler {

    @Inject
    private AuthTokenService authTokenService;

    // Esecuzione: ogni Domenica alle 03:00:00, ora italiana.
    // Si attiva all'avvio dell'applicazione.
    @Asynchronous(runAt = @Schedule(cron = "0 0 3 * * SUN", zone = "Europe/Rome"))
    public void deleteExpiredAuthTokens(@Observes Startup event) {
        try {
            System.out
                    .println("[AuthTokenCleanupJob] Started: removing expired tokens");

            int deletedTokens = authTokenService.deleteExpiredAuthTokens();

            System.out.println("[AuthTokenCleanupJob] Completed: " + deletedTokens + " expired tokens removed");
        } catch (ContextNotActiveException e) {
            System.err.println(
                    "[AuthTokenCleanupJob] Stopped: the application that scheduled this job is no longer active");
            throw e;
        } catch (RuntimeException e) {
            System.err.println(
                    "[AuthTokenCleanupJob] Failed: no token was removed, retrying at the next scheduled run");
            e.printStackTrace();
        }
    }
}