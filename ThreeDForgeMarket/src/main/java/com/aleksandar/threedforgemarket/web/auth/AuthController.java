package com.aleksandar.threedforgemarket.web.auth;

import com.aleksandar.threedforgemarket.exception.auth.EmailAlreadyExistsException;
import com.aleksandar.threedforgemarket.exception.auth.InvalidLoginCredentialsException;
import com.aleksandar.threedforgemarket.exception.auth.PasswordsDoNotMatchException;
import com.aleksandar.threedforgemarket.exception.auth.UsernameAlreadyExistsException;
import com.aleksandar.threedforgemarket.model.dto.auth.LoginRequest;
import com.aleksandar.threedforgemarket.model.dto.auth.RegisterRequest;
import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.service.user.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    public static final String USER_ID_SESSION_ATTRIBUTE = "user_id";

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/auth/register")
    public ModelAndView getRegisterPage(HttpSession session) {
        if (isAuthenticated(session)) {
            return new ModelAndView("redirect:/");
        }

        ModelAndView modelAndView = new ModelAndView("auth/register");
        modelAndView.addObject("registerForm", new RegisterRequest());

        return modelAndView;
    }

    @PostMapping("/auth/register")
    public ModelAndView register(
            @Valid @ModelAttribute("registerForm") RegisterRequest registerRequest,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (isAuthenticated(session)) {
            return new ModelAndView("redirect:/");
        }

        if (bindingResult.hasErrors()) {
            return new ModelAndView("auth/register");
        }

        try {
            userService.register(registerRequest);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Your account was created successfully. You can now log in."
            );

            return new ModelAndView("redirect:/auth/login");
        }
        catch (PasswordsDoNotMatchException exception) {
            bindingResult.rejectValue(
                    "confirmPassword",
                    "password.mismatch",
                    exception.getMessage()
            );
        }
        catch (UsernameAlreadyExistsException exception) {
            bindingResult.rejectValue(
                    "username",
                    "username.exists",
                    exception.getMessage()
            );
        }
        catch (EmailAlreadyExistsException exception) {
            bindingResult.rejectValue(
                    "email",
                    "email.exists",
                    exception.getMessage()
            );
        }

        return new ModelAndView("auth/register");
    }

    @GetMapping("/auth/login")
    public ModelAndView getLoginPage(HttpSession session) {
        if (isAuthenticated(session)) {
            return new ModelAndView("redirect:/");
        }

        ModelAndView modelAndView = new ModelAndView("auth/login");
        modelAndView.addObject("loginForm", new LoginRequest());

        return modelAndView;
    }

    @PostMapping("/auth/login")
    public ModelAndView login(
            @Valid @ModelAttribute("loginForm") LoginRequest loginRequest,
            BindingResult bindingResult,
            HttpSession session
    ) {
        if (isAuthenticated(session)) {
            return new ModelAndView("redirect:/");
        }

        if (bindingResult.hasErrors()) {
            return new ModelAndView("auth/login");
        }

        try {
            User loggedInUser = userService.login(loginRequest);

            session.setAttribute(
                    USER_ID_SESSION_ATTRIBUTE,
                    loggedInUser.getId()
            );

            return new ModelAndView("redirect:/");
        }
        catch (InvalidLoginCredentialsException exception) {
            bindingResult.rejectValue(
                    "usernameOrEmail",
                    "login.invalid",
                    exception.getMessage()
            );

            return new ModelAndView("auth/login");
        }
    }

    @PostMapping("/logout")
    public ModelAndView logout(
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        session.invalidate();

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "You have been logged out successfully.");

        return new ModelAndView("redirect:/auth/login");
    }

    private boolean isAuthenticated(HttpSession session) {
        return session.getAttribute(USER_ID_SESSION_ATTRIBUTE) != null;
    }
}
