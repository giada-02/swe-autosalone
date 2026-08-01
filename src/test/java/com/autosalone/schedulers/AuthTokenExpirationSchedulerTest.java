package com.autosalone.schedulers;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autosalone.services.AuthTokenService;

@ExtendWith(MockitoExtension.class)
class AuthTokenExpirationSchedulerTest {

    @Mock
    private AuthTokenService authTokenService;

    @InjectMocks
    private AuthTokenExpirationScheduler scheduler;

    @Test
    void deleteExpiredAuthTokens_CallsServiceMethod() {
        scheduler.deleteExpiredAuthTokens();
        verify(authTokenService).deleteExpiredAuthTokens();
    }
}