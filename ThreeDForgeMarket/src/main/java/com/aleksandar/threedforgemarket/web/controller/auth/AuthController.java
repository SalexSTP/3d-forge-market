package com.aleksandar.threedforgemarket.web.controller.auth;

import com.aleksandar.threedforgemarket.exception.auth.EmailAlreadyExistsException;
import com.aleksandar.threedforgemarket.exception.auth.PasswordsDoNotMatchException;
import com.aleksandar.threedforgemarket.exception.auth.UsernameAlreadyExistsException;
import com.aleksandar.threedforgemarket.model.dto.auth.LoginRequest;
import com.aleksandar.threedforgemarket.model.dto.auth.RegisterRequest;
import com.aleksandar.threedforgemarket.service.user.UserService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/auth/register")
    public ModelAndView getRegisterPage(Authentication authentication) {
        if (isAuthenticated(authentication)) {
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
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        if (isAuthenticated(authentication)) {
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
    public ModelAndView getLoginPage(
            Authentication authentication,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout
    ) {
        if (isAuthenticated(authentication)) {
            return new ModelAndView("redirect:/");
        }

        ModelAndView modelAndView = new ModelAndView("auth/login");
        modelAndView.addObject("loginForm", new LoginRequest());

        if (error != null) {
            modelAndView.addObject(
                    "errorMessage",
                    "Invalid username, email, or password."
            );
        }

        if (logout != null) {
            modelAndView.addObject(
                    "successMessage",
                    "You have been logged out successfully."
            );
        }

        return modelAndView;
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
