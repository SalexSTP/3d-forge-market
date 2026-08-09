package com.aleksandar.threedforgemarket.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MarketplaceAuthenticationFailureHandler implements AuthenticationFailureHandler {
    private final SimpleUrlAuthenticationFailureHandler disabledAccountFailureHandler;
    private final SimpleUrlAuthenticationFailureHandler genericFailureHandler;

    public MarketplaceAuthenticationFailureHandler() {
        this.disabledAccountFailureHandler = new SimpleUrlAuthenticationFailureHandler("/auth/login?disabled");
        this.genericFailureHandler = new SimpleUrlAuthenticationFailureHandler("/auth/login?error");
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        if (exception instanceof DisabledException) {
            disabledAccountFailureHandler.onAuthenticationFailure(request, response, exception);
            return;
        }

        genericFailureHandler.onAuthenticationFailure(request, response, exception);
    }
}
