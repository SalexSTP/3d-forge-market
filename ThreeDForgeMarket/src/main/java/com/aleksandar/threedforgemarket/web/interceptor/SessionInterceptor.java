package com.aleksandar.threedforgemarket.web.interceptor;

import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.enums.user.UserRole;
import com.aleksandar.threedforgemarket.service.user.UserService;
import com.aleksandar.threedforgemarket.web.controller.auth.AuthController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
public class SessionInterceptor implements HandlerInterceptor {
    private final UserService userService;

    public SessionInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws IOException {
        HttpSession session = request.getSession(false);

        if (session == null) {
            redirectToLogin(request, response);
            return false;
        }

        Object sessionUserId = session.getAttribute(
                AuthController.USER_ID_SESSION_ATTRIBUTE
        );

        if (!(sessionUserId instanceof UUID userId)) {
            session.invalidate();
            redirectToLogin(request, response);
            return false;
        }

        Optional<User> currentUser = userService.findById(userId);

        if (currentUser.isEmpty()) {
            session.invalidate();
            redirectToLogin(request, response);
            return false;
        }

        String requestPath = request.getRequestURI()
                .substring(request.getContextPath().length());

        if ((requestPath.equals("/admin") || requestPath.startsWith("/admin/"))
                && currentUser.get().getRole() != UserRole.ADMIN) {

            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        if ((requestPath.equals("/orders") || requestPath.startsWith("/orders/"))
                && currentUser.get().getRole() != UserRole.CUSTOMER) {

            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        return true;
    }

    private void redirectToLogin(HttpServletRequest request,
                                 HttpServletResponse response) throws IOException {
        response.sendRedirect(
                request.getContextPath() + "/auth/login"
        );
    }
}
