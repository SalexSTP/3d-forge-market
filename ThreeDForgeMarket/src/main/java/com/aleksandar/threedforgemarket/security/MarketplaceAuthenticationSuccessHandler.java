package com.aleksandar.threedforgemarket.security;

import com.aleksandar.threedforgemarket.service.user.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MarketplaceAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final UserService userService;
    private final SavedRequestAwareAuthenticationSuccessHandler delegate;

    public MarketplaceAuthenticationSuccessHandler(UserService userService) {
        this.userService = userService;
        this.delegate = new SavedRequestAwareAuthenticationSuccessHandler();
        this.delegate.setDefaultTargetUrl("/");
        this.delegate.setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        if (authentication.getPrincipal() instanceof MarketplaceUserDetails currentUser) {
            userService.updateLastLogin(currentUser.getId());
        }

        delegate.onAuthenticationSuccess(request, response, authentication);
    }
}
