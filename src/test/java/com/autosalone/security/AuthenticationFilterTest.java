package com.autosalone.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.autosalone.exceptions.ResourceNotFoundException;
import com.autosalone.models.User;
import com.autosalone.services.JwtService;
import com.autosalone.services.UserService;
import com.autosalone.utils.AuditContext;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    private static final String TOKEN = "jwt-token";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant ISSUED_AT = Instant.parse("2026-09-15T10:00:00Z");

    @Mock
    private JwtService jwtService;

    @Mock
    private UserService userService;

    @Mock
    private ContainerRequestContext requestContext;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Claim roleClaim;

    @Mock
    private DecodedJWT decodedJWT;

    @Mock
    private User user;

    @InjectMocks
    private AuthenticationFilter filter;

    @AfterEach
    void clearAuditContext() {
        AuditContext.clear();
    }

    @Test
    void filter_WithoutAuthorizationHeader_LetsRequestThrough() {
        filter.filter(requestContext);

        verify(jwtService, never()).validateToken(any());
        verify(requestContext, never()).abortWith(any());
    }

    @Test
    void filter_WithNonBearerHeader_LetsRequestThrough() {
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Non Bearer Token");

        filter.filter(requestContext);

        verify(jwtService, never()).validateToken(any());
        verify(requestContext, never()).abortWith(any());
    }

    @Test
    void filter_WithValidTokenAndActiveUser_SetsSecurityContext() {
        givenSignedToken();
        when(userService.getUserById(USER_ID)).thenReturn(user);
        when(user.isActive()).thenReturn(true);
        when(decodedJWT.getIssuedAtAsInstant()).thenReturn(ISSUED_AT);
        when(user.acceptsTokenIssuedAt(ISSUED_AT)).thenReturn(true);
        when(requestContext.getSecurityContext()).thenReturn(securityContext);

        filter.filter(requestContext);

        ArgumentCaptor<SecurityContext> captor = ArgumentCaptor.forClass(SecurityContext.class);
        verify(requestContext).setSecurityContext(captor.capture());
        SecurityContext securityContext = captor.getValue();

        assertEquals(USER_ID.toString(), securityContext.getUserPrincipal().getName());
        assertTrue(securityContext.isUserInRole("CUSTOMER"));
        assertFalse(securityContext.isUserInRole("OWNER"));
        assertEquals(USER_ID, AuditContext.getCurrentUserId());
        verify(requestContext, never()).abortWith(any());
    }

    @Test
    void filter_WithInvalidToken_Returns401() {
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + TOKEN);
        when(jwtService.validateToken(TOKEN)).thenThrow(new JWTVerificationException("Invalid signature"));

        filter.filter(requestContext);

        assertEquals(401, abortedStatus());
        verify(userService, never()).getUserById(any());
    }

    @Test
    void filter_WhenUserDoesNotExist_Returns401() {
        givenSignedToken();
        when(userService.getUserById(USER_ID)).thenThrow(new ResourceNotFoundException("User not found"));

        filter.filter(requestContext);

        assertEquals(401, abortedStatus());
        verify(requestContext, never()).setSecurityContext(any());
    }

    @Test
    void filter_WhenUserIsInactive_Returns401() {
        givenSignedToken();
        when(userService.getUserById(USER_ID)).thenReturn(user);
        when(user.isActive()).thenReturn(false);

        filter.filter(requestContext);

        assertEquals(401, abortedStatus());
        verify(requestContext, never()).setSecurityContext(any());
        assertEquals(AuditContext.SYSTEM_ID, AuditContext.getCurrentUserId(),
                "A rejected user must not be recorded as the current user");
    }

    @Test
    void filter_WithTokenIssuedBeforeInvalidation_Returns401() {
        givenSignedToken();
        when(userService.getUserById(USER_ID)).thenReturn(user);
        when(user.isActive()).thenReturn(true);
        when(decodedJWT.getIssuedAtAsInstant()).thenReturn(ISSUED_AT);
        when(user.acceptsTokenIssuedAt(ISSUED_AT)).thenReturn(false);

        filter.filter(requestContext);

        assertEquals(401, abortedStatus());
        verify(requestContext, never()).setSecurityContext(any());
    }

    // helper methods

    private void givenSignedToken() {
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + TOKEN);
        when(jwtService.validateToken(TOKEN)).thenReturn(decodedJWT);
        when(decodedJWT.getSubject()).thenReturn(USER_ID.toString());
        when(decodedJWT.getClaim("role")).thenReturn(roleClaim);
        when(roleClaim.asString()).thenReturn("CUSTOMER");
    }

    private int abortedStatus() {
        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(captor.capture());
        return captor.getValue().getStatus();
    }
}