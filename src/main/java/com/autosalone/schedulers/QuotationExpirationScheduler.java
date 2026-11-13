package com.autosalone.schedulers;

import com.autosalone.config.ClockProducer;
import com.autosalone.dtos.responses.QuotationCleanupResponse;
import com.autosalone.services.QuotationService;

import jakarta.enterprise.concurrent.Asynchronous;
import jakarta.enterprise.concurrent.Schedule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;

@ApplicationScoped
public class QuotationExpirationScheduler {

    @Inject
    private QuotationService quotationService;

    // Esecuzione: tutti i giorni alle 00:01, ora italiana.
    // Si attiva all'avvio dell'applicazione.
    @Asynchronous(runAt = @Schedule(cron = "0 1 0 * * *", zone = ClockProducer.TIME_ZONE))
    public void checkExpiredQuotations(@Observes Startup event) {
        try {
            System.out.println("[QuotationExpirationJob] Started: checking for quotations past their expiration date");

            QuotationCleanupResponse result = quotationService.expireOutdatedQuotations();

            System.out.println(
                    "[QuotationExpirationJob] Completed: " + result.expiredQuotations() + " quotations expired, "
                            + result.voidedContracts() + " draft contracts voided, "
                            + result.freedVehicles() + " vehicles made available");
        } catch (ContextNotActiveException e) {
            System.err.println(
                    "[QuotationExpirationJob] Stopped: the application that scheduled this job is no longer active");
            throw e;
        } catch (RuntimeException e) {
            System.err.println(
                    "[QuotationExpirationJob] Failed: no quotation was expired, retrying at the next scheduled run");
            e.printStackTrace();
        }
    }
}
