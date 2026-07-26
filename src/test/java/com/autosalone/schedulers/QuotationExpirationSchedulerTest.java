package com.autosalone.schedulers;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autosalone.services.QuotationService;

@ExtendWith(MockitoExtension.class)
class QuotationExpirationSchedulerTest {

    @Mock
    private QuotationService quotationService;

    @InjectMocks
    private QuotationExpirationScheduler scheduler;

    @Test
    void checkExpiredQuotations_CallsServiceMethod() {
        scheduler.checkExpiredQuotations();
        verify(quotationService).expireOutdatedQuotations();
    }
}