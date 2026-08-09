package com.aleksandar.threedforgemarket.web.controller.customprint;

import com.aleksandar.threedforgemarket.exception.customprint.CustomPrintRequestNotFoundException;
import com.aleksandar.threedforgemarket.exception.customprint.CustomPrintRequestOperationFailedException;
import com.aleksandar.threedforgemarket.exception.customprint.CustomPrintServiceUnavailableException;
import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestDetailsClientDto;
import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestStatus;
import com.aleksandar.threedforgemarket.model.dto.customprint.CustomPrintChangeRequestFormDto;
import com.aleksandar.threedforgemarket.model.dto.customprint.CustomPrintRequestFormDto;
import com.aleksandar.threedforgemarket.model.dto.customprint.CustomPrintSearchRequest;
import com.aleksandar.threedforgemarket.model.enums.payment.PaymentTargetType;
import com.aleksandar.threedforgemarket.security.MarketplaceUserDetails;
import com.aleksandar.threedforgemarket.service.customprint.CustomPrintRequestService;
import com.aleksandar.threedforgemarket.service.payment.PaymentService;
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
    private final PaymentService paymentService;

    public CustomPrintRequestController(
            CustomPrintRequestService customPrintRequestService,
            PaymentService paymentService
    ) {
        this.customPrintRequestService = customPrintRequestService;
        this.paymentService = paymentService;
    }

    @ModelAttribute("statuses")
    public CustomPrintRequestStatus[] statuses() {
        return CustomPrintRequestStatus.values();
    }

    @GetMapping
    public ModelAndView getCustomerRequestsPage(
            @ModelAttribute("searchRequest") CustomPrintSearchRequest searchRequest,
            @AuthenticationPrincipal MarketplaceUserDetails currentUser
    ) {
        ModelAndView modelAndView = new ModelAndView("custom-print/list");
        modelAndView.addObject("searchRequest", searchRequest);

        try {
            modelAndView.addObject(
                    "requests",
                    customPrintRequestService.getCustomerRequests(currentUser.getId(), searchRequest)
            );
        } catch (CustomPrintServiceUnavailableException exception) {
            modelAndView.addObject("requests", List.of());
            modelAndView.addObject("errorMessage", exception.getMessage());
            modelAndView.addObject("serviceUnavailable", true);
        } catch (CustomPrintRequestOperationFailedException exception) {
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
            return customerDetailsModelAndView(
                    customPrintRequestService.getCustomerRequestDetails(currentUser.getId(), id),
                    new CustomPrintChangeRequestFormDto(),
                    null
            );

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

    @GetMapping("/{id}/edit")
    public ModelAndView getEditRequestPage(
            @PathVariable UUID id,
            @AuthenticationPrincipal MarketplaceUserDetails currentUser,
            RedirectAttributes redirectAttributes
    ) {
        try {
            return editRequestModelAndView(
                    id,
                    customPrintRequestService.getEditForm(currentUser.getId(), id)
            );
        } catch (CustomPrintRequestNotFoundException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "That custom print request could not be found.");
            return new ModelAndView("redirect:/custom-prints");
        } catch (CustomPrintRequestOperationFailedException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        } catch (CustomPrintServiceUnavailableException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return new ModelAndView("redirect:/custom-prints");
        }

        return new ModelAndView("redirect:/custom-prints/" + id);
    }

    @PutMapping("/{id}")
    public ModelAndView updateRequest(
            @PathVariable UUID id,
            @Valid @ModelAttribute("requestForm") CustomPrintRequestFormDto requestForm,
            BindingResult bindingResult,
            @AuthenticationPrincipal MarketplaceUserDetails currentUser,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return editRequestModelAndView(id, requestForm);
        }

        try {
            customPrintRequestService.updateCustomerRequest(currentUser.getId(), id, requestForm);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Your custom print request was updated successfully."
            );
            return new ModelAndView("redirect:/custom-prints/" + id);
        } catch (CustomPrintRequestNotFoundException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "That custom print request could not be found.");
            return new ModelAndView("redirect:/custom-prints");
        } catch (CustomPrintRequestOperationFailedException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only pending custom print requests can be edited.");
        } catch (CustomPrintServiceUnavailableException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return new ModelAndView("redirect:/custom-prints/" + id);
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

    @PutMapping("/{id}/accept")
    public ModelAndView acceptOffer(
            @PathVariable UUID id,
            @AuthenticationPrincipal MarketplaceUserDetails currentUser,
            RedirectAttributes redirectAttributes
    ) {
        return new ModelAndView("redirect:/payments/custom-prints/" + id);
    }

    @PutMapping("/{id}/request-changes")
    public ModelAndView requestChanges(
            @PathVariable UUID id,
            @Valid @ModelAttribute("changeRequestForm") CustomPrintChangeRequestFormDto changeRequestForm,
            BindingResult bindingResult,
            @AuthenticationPrincipal MarketplaceUserDetails currentUser,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            try {
                return customerDetailsModelAndView(
                        customPrintRequestService.getCustomerRequestDetails(currentUser.getId(), id),
                        changeRequestForm,
                        "change-request-modal"
                );
            } catch (CustomPrintServiceUnavailableException exception) {
                redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
                return new ModelAndView("redirect:/custom-prints");
            }
        }

        try {
            customPrintRequestService.requestChanges(currentUser.getId(), id, changeRequestForm);
            redirectAttributes.addFlashAttribute("successMessage", "Your change request was sent.");
        } catch (CustomPrintRequestNotFoundException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "That custom print request could not be found.");
        } catch (CustomPrintRequestOperationFailedException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "Changes can no longer be requested for this offer.");
        } catch (CustomPrintServiceUnavailableException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return new ModelAndView("redirect:/custom-prints/" + id);
    }

    @PutMapping("/{id}/hide")
    public ModelAndView hideRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal MarketplaceUserDetails currentUser,
            RedirectAttributes redirectAttributes
    ) {
        try {
            customPrintRequestService.hideCustomerRequest(currentUser.getId(), id);
            redirectAttributes.addFlashAttribute("successMessage", "The custom print request was removed from your history.");
        } catch (CustomPrintRequestNotFoundException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "That custom print request could not be found.");
        } catch (CustomPrintRequestOperationFailedException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only cancelled, rejected, or delivered requests can be removed.");
        } catch (CustomPrintServiceUnavailableException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return new ModelAndView("redirect:/custom-prints");
    }

    private ModelAndView customerDetailsModelAndView(
            CustomPrintRequestDetailsClientDto request,
            CustomPrintChangeRequestFormDto changeRequestForm,
            String openModal
    ) {
        ModelAndView modelAndView = new ModelAndView("custom-print/details");
        modelAndView.addObject("request", request);
        modelAndView.addObject("changeRequestForm", changeRequestForm);
        modelAndView.addObject("openModal", openModal);
        modelAndView.addObject("paymentSummary", paymentService
                .getLatestPaymentSummary(
                        PaymentTargetType.CUSTOM_PRINT_REQUEST,
                        request.id(),
                        isCancelledBeforePayment(request.status())
                )
                .orElse(null));

        return modelAndView;
    }

    private boolean isCancelledBeforePayment(CustomPrintRequestStatus status) {
        return status == CustomPrintRequestStatus.CANCELLED
                || status == CustomPrintRequestStatus.REJECTED;
    }

    private ModelAndView editRequestModelAndView(
            UUID requestId,
            CustomPrintRequestFormDto requestForm
    ) {
        ModelAndView modelAndView = new ModelAndView("custom-print/edit");
        modelAndView.addObject("requestId", requestId);
        modelAndView.addObject("requestForm", requestForm);

        return modelAndView;
    }
}
