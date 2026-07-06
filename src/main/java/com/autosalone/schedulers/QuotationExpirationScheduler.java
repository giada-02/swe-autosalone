package com.autosalone.schedulers;

import com.autosalone.services.QuotationService;

import jakarta.enterprise.concurrent.Asynchronous;
import jakarta.enterprise.concurrent.Schedule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class QuotationExpirationScheduler {

    @Inject
    private QuotationService quotationService;

    @Asynchronous(runAt = @Schedule(cron = "0 1 0 * * *"))
    public void checkExpiredQuotations() {
        System.out.println("Avvio job di background (Concurrency API): Verifica preventivi scaduti...");

        quotationService.expireOutdatedQuotations();

        System.out.println("Job verifica preventivi completato");
    }
}
