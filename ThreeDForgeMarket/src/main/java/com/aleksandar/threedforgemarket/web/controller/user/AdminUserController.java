package com.aleksandar.threedforgemarket.web.controller.user;

import com.aleksandar.threedforgemarket.exception.auth.UserOperationNotAllowedException;
import com.aleksandar.threedforgemarket.model.enums.user.UserRole;
import com.aleksandar.threedforgemarket.model.user.AdminUserSearchRequest;
import com.aleksandar.threedforgemarket.model.user.UpdateUserRoleRequest;
import com.aleksandar.threedforgemarket.security.MarketplaceUserDetails;
import com.aleksandar.threedforgemarket.service.user.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {
    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ModelAndView getUserManagementPage(
            @ModelAttribute("searchRequest") AdminUserSearchRequest searchRequest,
            @AuthenticationPrincipal MarketplaceUserDetails currentUser
    ) {
        ModelAndView modelAndView = new ModelAndView("admin/users");

        modelAndView.addObject(
                "users",
                userService.getUsersForAdmin(searchRequest, currentUser.getId())
        );
        modelAndView.addObject("roles", UserRole.values());

        return modelAndView;
    }

    @PutMapping("/{id}/role")
    public ModelAndView changeUserRole(
            @PathVariable UUID id,
            @Valid @ModelAttribute UpdateUserRoleRequest roleRequest,
            BindingResult bindingResult,
            @AuthenticationPrincipal MarketplaceUserDetails currentUser,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please choose a valid role."
            );

            return redirectToUsers();
        }

        try {
            userService.changeUserRole(
                    id,
                    roleRequest.getRole(),
                    currentUser.getId()
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "User role was updated successfully."
            );

        } catch (UserOperationNotAllowedException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return redirectToUsers();
    }

    @PutMapping("/{id}/deactivate")
    public ModelAndView deactivateUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal MarketplaceUserDetails currentUser,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userService.deactivateUser(id, currentUser.getId());

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "User account was deactivated successfully."
            );

        } catch (UserOperationNotAllowedException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return redirectToUsers();
    }

    @PutMapping("/{id}/reactivate")
    public ModelAndView reactivateUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal MarketplaceUserDetails currentUser,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userService.reactivateUser(id, currentUser.getId());

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "User account was reactivated successfully."
            );

        } catch (UserOperationNotAllowedException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return redirectToUsers();
    }

    private ModelAndView redirectToUsers() {
        return new ModelAndView("redirect:/admin/users");
    }
}
