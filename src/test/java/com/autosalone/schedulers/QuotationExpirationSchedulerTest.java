package com.autosalone.schedulers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autosalone.dtos.responses.QuotationCleanupResponse;
import com.autosalone.services.QuotationService;

import jakarta.enterprise.context.ContextNotActiveException;

@ExtendWith(MockitoExtension.class)
class QuotationExpirationSchedulerTest {

    @Mock
    private QuotationService quotationService;

    @InjectMocks
    private QuotationExpirationScheduler scheduler;

    @Test
    void checkExpiredQuotations_CallsServiceMethod() {
        when(quotationService.expireOutdatedQuotations()).thenReturn(new QuotationCleanupResponse(2, 1, 1));

        scheduler.checkExpiredQuotations(null);

        verify(quotationService).expireOutdatedQuotations();
    }

    @Test
    void checkExpiredQuotations_ContextNotActiveException_ThrowsException() {
        when(quotationService.expireOutdatedQuotations())
                .thenThrow(new ContextNotActiveException());

        assertThrows(ContextNotActiveException.class, () -> scheduler.checkExpiredQuotations(null));

        verify(quotationService).expireOutdatedQuotations();
    }

    @Test
    void checkExpiredQuotations_RuntimeException_DoesNotPropagateException() {
        when(quotationService.expireOutdatedQuotations())
                .thenThrow(new RuntimeException());

        assertDoesNotThrow(() -> scheduler.checkExpiredQuotations(null));

        verify(quotationService).expireOutdatedQuotations();
    }

}