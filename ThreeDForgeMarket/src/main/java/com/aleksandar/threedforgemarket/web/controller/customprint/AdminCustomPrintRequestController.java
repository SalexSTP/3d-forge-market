package com.aleksandar.threedforgemarket.web.controller.customprint;

import com.aleksandar.threedforgemarket.exception.customprint.CustomPrintRequestNotFoundException;
import com.aleksandar.threedforgemarket.exception.customprint.CustomPrintRequestOperationFailedException;
import com.aleksandar.threedforgemarket.exception.customprint.CustomPrintServiceUnavailableException;
import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestDetailsClientDto;
import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestStatus;
import com.aleksandar.threedforgemarket.model.dto.customprint.CustomPrintFulfillmentStatusFormDto;
import com.aleksandar.threedforgemarket.model.dto.customprint.CustomPrintOfferFormDto;
import com.aleksandar.threedforgemarket.model.dto.customprint.CustomPrintRejectFormDto;
import com.aleksandar.threedforgemarket.model.dto.customprint.CustomPrintSearchRequest;
import com.aleksandar.threedforgemarket.service.customprint.CustomPrintRequestService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/custom-prints")
public class AdminCustomPrintRequestController {
    private final CustomPrintRequestService customPrintRequestService;

    public AdminCustomPrintRequestController(CustomPrintRequestService customPrintRequestService) {
        this.customPrintRequestService = customPrintRequestService;
    }

    @ModelAttribute("statuses")
    public CustomPrintRequestStatus[] statuses() {
        return CustomPrintRequestStatus.values();
    }

    @GetMapping
    public ModelAndView getAdminRequestsPage(
            @ModelAttribute("searchRequest") CustomPrintSearchRequest searchRequest
    ) {
        ModelAndView modelAndView = new ModelAndView("admin/custom-prints");
        modelAndView.addObject("searchRequest", searchRequest);

        try {
            modelAndView.addObject(
                    "requests",
                    customPrintRequestService.getAllRequestsForAdmin(searchRequest)
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

    @GetMapping("/{id}")
    public ModelAndView getAdminRequestDetailsPage(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            CustomPrintRequestDetailsClientDto request = customPrintRequestService.getRequestDetailsForAdmin(id);

            return adminDetailsModelAndView(
                    request,
                    toOfferForm(request),
                    new CustomPrintRejectFormDto(),
                    new CustomPrintFulfillmentStatusFormDto(),
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

        return new ModelAndView("redirect:/admin/custom-prints");
    }

    @PutMapping("/{id}/offer")
    public ModelAndView sendOffer(
            @PathVariable UUID id,
            @Valid @ModelAttribute("offerForm") CustomPrintOfferFormDto offerForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            try {
                CustomPrintRequestDetailsClientDto request = customPrintRequestService.getRequestDetailsForAdmin(id);

                return adminDetailsModelAndView(
                        request,
                        offerForm,
                        new CustomPrintRejectFormDto(),
                        new CustomPrintFulfillmentStatusFormDto(),
                        "offer-modal"
                );
            } catch (CustomPrintServiceUnavailableException exception) {
                redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
                return new ModelAndView("redirect:/admin/custom-prints");
            }
        }

        try {
            customPrintRequestService.sendOffer(id, offerForm);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "The custom print offer was sent."
            );

        } catch (CustomPrintRequestNotFoundException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "That custom print request could not be found."
            );

        } catch (CustomPrintRequestOperationFailedException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This custom print request can no longer receive an offer."
            );

        } catch (CustomPrintServiceUnavailableException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return new ModelAndView("redirect:/admin/custom-prints/" + id);
    }

    @PutMapping("/{id}/reject")
    public ModelAndView rejectRequest(
            @PathVariable UUID id,
            @Valid @ModelAttribute("rejectForm") CustomPrintRejectFormDto rejectForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            try {
                CustomPrintRequestDetailsClientDto request = customPrintRequestService.getRequestDetailsForAdmin(id);

                return adminDetailsModelAndView(
                        request,
                        toOfferForm(request),
                        rejectForm,
                        new CustomPrintFulfillmentStatusFormDto(),
                        "reject-modal"
                );
            } catch (CustomPrintServiceUnavailableException exception) {
                redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
                return new ModelAndView("redirect:/admin/custom-prints");
            }
        }

        try {
            customPrintRequestService.rejectRequest(id, rejectForm);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "The custom print request was rejected."
            );

        } catch (CustomPrintRequestNotFoundException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "That custom print request could not be found."
            );

        } catch (CustomPrintRequestOperationFailedException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This custom print request can no longer be rejected."
            );

        } catch (CustomPrintServiceUnavailableException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return new ModelAndView("redirect:/admin/custom-prints/" + id);
    }

    @PutMapping("/{id}/fulfillment-status")
    public ModelAndView updateFulfillmentStatus(
            @PathVariable UUID id,
            @Valid @ModelAttribute("fulfillmentStatusForm") CustomPrintFulfillmentStatusFormDto fulfillmentStatusForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please choose a valid fulfillment status.");
            return new ModelAndView("redirect:/admin/custom-prints/" + id);
        }

        try {
            customPrintRequestService.updateFulfillmentStatus(id, fulfillmentStatusForm);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Custom print status was updated successfully."
            );
        } catch (CustomPrintRequestNotFoundException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "That custom print request could not be found.");
        } catch (CustomPrintRequestOperationFailedException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "This custom print status cannot be updated that way.");
        } catch (CustomPrintServiceUnavailableException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return new ModelAndView("redirect:/admin/custom-prints/" + id);
    }

    @PutMapping("/{id}/archive")
    public ModelAndView archiveRequest(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            customPrintRequestService.archiveRequest(id);
            redirectAttributes.addFlashAttribute("successMessage", "The custom print request was archived.");
            return new ModelAndView("redirect:/admin/custom-prints");
        } catch (CustomPrintRequestNotFoundException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "That custom print request could not be found.");
        } catch (CustomPrintRequestOperationFailedException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only cancelled, rejected, or delivered requests can be archived.");
        } catch (CustomPrintServiceUnavailableException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return new ModelAndView("redirect:/admin/custom-prints/" + id);
    }

    private ModelAndView adminDetailsModelAndView(
            CustomPrintRequestDetailsClientDto request,
            CustomPrintOfferFormDto offerForm,
            CustomPrintRejectFormDto rejectForm,
            CustomPrintFulfillmentStatusFormDto fulfillmentStatusForm,
            String openModal
    ) {
        ModelAndView modelAndView = new ModelAndView("admin/custom-print-details");
        modelAndView.addObject("request", request);
        modelAndView.addObject("offerForm", offerForm);
        modelAndView.addObject("rejectForm", rejectForm);
        modelAndView.addObject("fulfillmentStatusForm", fulfillmentStatusForm);
        modelAndView.addObject("openModal", openModal);

        return modelAndView;
    }

    private CustomPrintOfferFormDto toOfferForm(CustomPrintRequestDetailsClientDto request) {
        CustomPrintOfferFormDto offerForm = new CustomPrintOfferFormDto();
        offerForm.setQuotedPrice(request.quotedPrice());
        offerForm.setEstimatedPrintTimeMinutes(request.estimatedPrintTimeMinutes());
        offerForm.setAdminMessage(request.adminMessage());
        offerForm.setResponseFileUrl(request.responseFileUrl());

        return offerForm;
    }
}
