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

import com.autosalone.services.AuthTokenService;

import jakarta.enterprise.context.ContextNotActiveException;

@ExtendWith(MockitoExtension.class)
class AuthTokenExpirationSchedulerTest {

    @Mock
    private AuthTokenService authTokenService;

    @InjectMocks
    private AuthTokenExpirationScheduler scheduler;

    @Test
    void deleteExpiredAuthTokens_CallsServiceMethod() {
        scheduler.deleteExpiredAuthTokens(null);
        verify(authTokenService).deleteExpiredAuthTokens();
    }

    @Test
    void deleteExpiredAuthTokens_ContextNotActiveException_ThrowsException() {
        when(authTokenService.deleteExpiredAuthTokens())
                .thenThrow(new ContextNotActiveException());

        assertThrows(ContextNotActiveException.class, () -> scheduler.deleteExpiredAuthTokens(null));

        verify(authTokenService).deleteExpiredAuthTokens();
    }

    @Test
    void deleteExpiredAuthTokens_RuntimeException_DoesNotPropagateException() {
        when(authTokenService.deleteExpiredAuthTokens())
                .thenThrow(new RuntimeException());

        assertDoesNotThrow(() -> scheduler.deleteExpiredAuthTokens(null));

        verify(authTokenService).deleteExpiredAuthTokens();
    }
}