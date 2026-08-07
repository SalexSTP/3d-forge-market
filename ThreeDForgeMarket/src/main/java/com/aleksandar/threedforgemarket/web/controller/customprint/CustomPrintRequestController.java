package com.aleksandar.threedforgemarket.web.controller.customprint;

import com.aleksandar.threedforgemarket.exception.customprint.CustomPrintRequestNotFoundException;
import com.aleksandar.threedforgemarket.exception.customprint.CustomPrintRequestOperationFailedException;
import com.aleksandar.threedforgemarket.exception.customprint.CustomPrintServiceUnavailableException;
import com.aleksandar.threedforgemarket.model.dto.customprint.CustomPrintRequestFormDto;
import com.aleksandar.threedforgemarket.security.MarketplaceUserDetails;
import com.aleksandar.threedforgemarket.service.customprint.CustomPrintRequestService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/custom-prints")
public class CustomPrintRequestController {
    private final CustomPrintRequestService customPrintRequestService;

    public CustomPrintRequestController(CustomPrintRequestService customPrintRequestService) {
        this.customPrintRequestService = customPrintRequestService;
    }

    @GetMapping
    public ModelAndView getCustomerRequestsPage(
            @AuthenticationPrincipal MarketplaceUserDetails currentUser
    ) {
        ModelAndView modelAndView = new ModelAndView("custom-print/list");

        try {
            modelAndView.addObject(
                    "requests",
                    customPrintRequestService.getCustomerRequests(currentUser.getId())
            );
        } catch (CustomPrintServiceUnavailableException exception) {
            modelAndView.addObject("requests", List.of());
            modelAndView.addObject("errorMessage", exception.getMessage());
        }

        return modelAndView;
    }

    @GetMapping("/new")
    public ModelAndView getCreateRequestPage() {
        ModelAndView modelAndView = new ModelAndView("custom-print/create");
        modelAndView.addObject("requestForm", new CustomPrintRequestFormDto());

        return modelAndView;
    }

    @PostMapping
    public ModelAndView createRequest(
            @Valid @ModelAttribute("requestForm") CustomPrintRequestFormDto requestForm,
            BindingResult bindingResult,
            @AuthenticationPrincipal MarketplaceUserDetails currentUser,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return new ModelAndView("custom-print/create");
        }

        try {
            customPrintRequestService.createCustomerRequest(
                    currentUser.getId(),
                    requestForm
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Your custom print request was submitted successfully."
            );

            return new ModelAndView("redirect:/custom-prints");

        } catch (CustomPrintRequestOperationFailedException
                 | CustomPrintServiceUnavailableException exception) {

            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());

            return new ModelAndView("redirect:/custom-prints/new");
        }
    }

    @GetMapping("/{id}")
    public ModelAndView getRequestDetailsPage(
            @PathVariable UUID id,
            @AuthenticationPrincipal MarketplaceUserDetails currentUser,
            RedirectAttributes redirectAttributes
    ) {
        try {
            ModelAndView modelAndView = new ModelAndView("custom-print/details");
            modelAndView.addObject(
                    "request",
                    customPrintRequestService.getCustomerRequestDetails(
                            currentUser.getId(),
                            id
                    )
            );

            return modelAndView;

        } catch (CustomPrintRequestNotFoundException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "That custom print request could not be found."
            );

        } catch (CustomPrintServiceUnavailableException
                 | CustomPrintRequestOperationFailedException exception) {

            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return new ModelAndView("redirect:/custom-prints");
    }

    @PutMapping("/{id}/cancel")
    public ModelAndView cancelRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal MarketplaceUserDetails currentUser,
            RedirectAttributes redirectAttributes
    ) {
        try {
            customPrintRequestService.cancelCustomerRequest(
                    currentUser.getId(),
                    id
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Your custom print request was cancelled."
            );

        } catch (CustomPrintRequestNotFoundException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "That custom print request could not be found."
            );

        } catch (CustomPrintRequestOperationFailedException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This custom print request can no longer be cancelled."
            );

        } catch (CustomPrintServiceUnavailableException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return new ModelAndView("redirect:/custom-prints");
    }
}
