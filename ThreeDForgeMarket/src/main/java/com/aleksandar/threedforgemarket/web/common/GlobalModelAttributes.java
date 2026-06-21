package com.aleksandar.threedforgemarket.web.common;

import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.enums.user.UserRole;
import com.aleksandar.threedforgemarket.service.user.UserService;
import com.aleksandar.threedforgemarket.web.controller.auth.AuthController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Optional;
import java.util.UUID;

@ControllerAdvice
public class GlobalModelAttributes {
    private final UserService userService;

    public GlobalModelAttributes(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute
    public void addAuthenticationAttributes(
            HttpServletRequest request,
            Model model
    ) {
        model.addAttribute("isAuthenticated", false);
        model.addAttribute("isAdmin", false);
        model.addAttribute("currentUsername", null);
        model.addAttribute("currentUserId", null);

        HttpSession session = request.getSession(false);

        if (session == null) {
            return;
        }

        Object sessionUserId = session.getAttribute(
                AuthController.USER_ID_SESSION_ATTRIBUTE
        );

        if (!(sessionUserId instanceof UUID userId)) {
            return;
        }

        Optional<User> currentUser = userService.findById(userId);

        if (currentUser.isEmpty()) {
            session.removeAttribute(AuthController.USER_ID_SESSION_ATTRIBUTE);
            return;
        }

        User user = currentUser.get();

        model.addAttribute("isAuthenticated", true);
        model.addAttribute("isAdmin",
                user.getRole() == UserRole.ADMIN);
        model.addAttribute("currentUsername", user.getUsername());
        model.addAttribute("currentUserId", user.getId());
    }
}
