package com.aleksandar.threedforgemarket.web.common;

import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.enums.user.UserRole;
import com.aleksandar.threedforgemarket.security.MarketplaceUserDetails;
import com.aleksandar.threedforgemarket.service.user.UserService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Optional;

@ControllerAdvice
public class GlobalModelAttributes {
    private final UserService userService;

    public GlobalModelAttributes(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute
    public void addAuthenticationAttributes(
            Authentication authentication,
            Model model
    ) {
        model.addAttribute("isAuthenticated", false);
        model.addAttribute("isAdmin", false);
        model.addAttribute("currentUsername", null);
        model.addAttribute("currentUserId", null);

        if (!isAuthenticated(authentication)
                || !(authentication.getPrincipal() instanceof MarketplaceUserDetails currentUser)) {
            return;
        }

        Optional<User> userById = userService.findById(currentUser.getId());

        if (userById.isEmpty()) {
            return;
        }

        User user = userById.get();

        model.addAttribute("isAuthenticated", true);
        model.addAttribute("isAdmin",
                user.getRole() == UserRole.ADMIN);
        model.addAttribute("currentUsername", user.getUsername());
        model.addAttribute("currentUserId", user.getId());
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
