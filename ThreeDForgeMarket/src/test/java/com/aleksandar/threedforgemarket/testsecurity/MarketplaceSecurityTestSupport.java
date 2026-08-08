package com.aleksandar.threedforgemarket.testsecurity;

import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.enums.user.UserRole;
import com.aleksandar.threedforgemarket.security.MarketplaceUserDetails;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

public final class MarketplaceSecurityTestSupport {

    private MarketplaceSecurityTestSupport() {
    }

    public static RequestPostProcessor customer(UUID id) {
        return marketplaceUser(id, "customer", "customer@example.com", UserRole.CUSTOMER);
    }

    public static RequestPostProcessor admin(UUID id) {
        return marketplaceUser(id, "admin", "admin@example.com", UserRole.ADMIN);
    }

    public static RequestPostProcessor marketplaceUser(
            UUID id,
            String username,
            String email,
            UserRole role
    ) {
        User userEntity = User.builder()
                .id(id)
                .username(username)
                .email(email)
                .password("{noop}password")
                .role(role)
                .active(true)
                .build();

        return user(new MarketplaceUserDetails(userEntity));
    }
}
