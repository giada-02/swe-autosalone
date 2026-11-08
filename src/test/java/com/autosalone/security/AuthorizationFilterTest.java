package com.autosalone.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.security.Principal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@ExtendWith(MockitoExtension.class)
class AuthorizationFilterTest {

    @Mock
    private ResourceInfo resourceInfo;
    @Mock
    private ContainerRequestContext requestContext;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Principal principal;

    @InjectMocks
    private AuthorizationFilter filter;

    static class FakeResourceController {
        public void unannotated() {
        }

        @PermitAll
        public void publicEndpoint() {
        }

        @RolesAllowed("OWNER")
        public void ownerOnly() {
        }
    }

    @Test
    void filter_WhenEndpointHasNoAnnotation_LetsRequestThrough() throws Exception {
        requestTo("unannotated");

        filter.filter(requestContext);

        verify(requestContext, never()).abortWith(any());
    }

    @Test
    void filter_WhenEndpointIsPermitAll_LetsRequestThrough() throws Exception {
        requestTo("publicEndpoint");

        filter.filter(requestContext);

        verify(requestContext, never()).abortWith(any());
    }

    @Test
    void filter_WhenUserIsNotAuthenticated_Returns401() throws Exception {
        requestTo("ownerOnly");
        when(requestContext.getSecurityContext()).thenReturn(securityContext);

        filter.filter(requestContext);

        assertEquals(401, abortedStatus());
    }

    @Test
    void filter_WhenUserHasRequiredRole_LetsRequestThrough() throws Exception {
        requestTo("ownerOnly");
        authenticated();
        when(securityContext.isUserInRole("OWNER")).thenReturn(true);

        filter.filter(requestContext);

        verify(requestContext, never()).abortWith(any());
    }

    @Test
    void filter_WhenUserLacksRequiredRole_Returns403() throws Exception {
        requestTo("ownerOnly");
        authenticated();
        when(securityContext.isUserInRole("OWNER")).thenReturn(false); // es. CUSTOMER

        filter.filter(requestContext);

        assertEquals(403, abortedStatus());
    }

    // helper methods

    private void requestTo(String methodName) throws NoSuchMethodException {
        Method method = FakeResourceController.class.getMethod(methodName);
        when(resourceInfo.getResourceMethod()).thenReturn(method);
        doReturn(FakeResourceController.class).when(resourceInfo).getResourceClass();
    }

    private void authenticated() {
        when(requestContext.getSecurityContext()).thenReturn(securityContext);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
    }

    private int abortedStatus() {
        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(captor.capture());
        return captor.getValue().getStatus();
    }
}