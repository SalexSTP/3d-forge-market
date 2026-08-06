package com.aleksandar.threedforgemarket.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

import static org.assertj.core.api.Assertions.assertThat;

class MarketplaceAuthenticationFailureHandlerTest {
    private final MarketplaceAuthenticationFailureHandler failureHandler =
            new MarketplaceAuthenticationFailureHandler();

    @Test
    void redirectsDisabledAccountsToDisabledLoginMessage() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(
                new MockHttpServletRequest(),
                response,
                new DisabledException("Account is disabled")
        );

        assertThat(response.getRedirectedUrl()).isEqualTo("/auth/login?disabled");
    }

    @Test
    void redirectsOtherFailuresToGenericLoginError() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(
                new MockHttpServletRequest(),
                response,
                new BadCredentialsException("Bad credentials")
        );

        assertThat(response.getRedirectedUrl()).isEqualTo("/auth/login?error");
    }
}
