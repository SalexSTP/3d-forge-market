package com.aleksandar.threedforgemarket.web.controller.user;

import com.aleksandar.threedforgemarket.exception.auth.EmailAlreadyExistsException;
import com.aleksandar.threedforgemarket.exception.auth.UsernameAlreadyExistsException;
import com.aleksandar.threedforgemarket.model.user.EditProfileRequest;
import com.aleksandar.threedforgemarket.service.user.UserService;
import com.aleksandar.threedforgemarket.web.controller.auth.AuthController;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
public class ProfileController {
    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ModelAndView getProfilePage(HttpSession session) {
        ModelAndView modelAndView = new ModelAndView("user/profile");

        modelAndView.addObject(
                "profile",
                userService.getCurrentUserProfile(getCurrentUserId(session))
        );

        return modelAndView;
    }

    @GetMapping("/profile/edit")
    public ModelAndView getEditProfilePage(HttpSession session) {
        ModelAndView modelAndView = new ModelAndView("user/edit-profile");

        modelAndView.addObject(
                "editProfileRequest",
                userService.getEditProfileRequest(getCurrentUserId(session))
        );

        return modelAndView;
    }

    @PutMapping("/profile")
    public ModelAndView updateProfile(
            @Valid @ModelAttribute("editProfileRequest")
            EditProfileRequest editProfileRequest,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return new ModelAndView("user/edit-profile");
        }

        try {
            userService.updateCurrentUserProfile(
                    getCurrentUserId(session),
                    editProfileRequest
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Your profile was updated successfully."
            );

            return new ModelAndView("redirect:/profile");

        } catch (UsernameAlreadyExistsException exception) {
            bindingResult.rejectValue(
                    "username",
                    "username.exists",
                    "This username is already taken."
            );

            return new ModelAndView("user/edit-profile");

        } catch (EmailAlreadyExistsException exception) {
            bindingResult.rejectValue(
                    "email",
                    "email.exists",
                    "This email is already registered."
            );

            return new ModelAndView("user/edit-profile");
        }
    }

    private UUID getCurrentUserId(HttpSession session) {
        Object sessionUserId = session.getAttribute(
                AuthController.USER_ID_SESSION_ATTRIBUTE
        );

        if (sessionUserId instanceof UUID userId) {
            return userId;
        }

        throw new IllegalStateException("Authenticated user session is required.");
    }
}
