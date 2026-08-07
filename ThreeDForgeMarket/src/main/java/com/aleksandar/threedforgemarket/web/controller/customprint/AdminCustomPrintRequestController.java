package com.aleksandar.threedforgemarket.web.controller.customprint;

import com.aleksandar.threedforgemarket.exception.customprint.CustomPrintRequestNotFoundException;
import com.aleksandar.threedforgemarket.exception.customprint.CustomPrintRequestOperationFailedException;
import com.aleksandar.threedforgemarket.exception.customprint.CustomPrintServiceUnavailableException;
import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestDetailsClientDto;
import com.aleksandar.threedforgemarket.model.dto.customprint.CustomPrintOfferFormDto;
import com.aleksandar.threedforgemarket.model.dto.customprint.CustomPrintRejectFormDto;
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

    @GetMapping
    public ModelAndView getAdminRequestsPage() {
        ModelAndView modelAndView = new ModelAndView("admin/custom-prints");

        try {
            modelAndView.addObject(
                    "requests",
                    customPrintRequestService.getAllRequestsForAdmin()
            );
        } catch (CustomPrintServiceUnavailableException exception) {
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
            return adminDetailsModelAndView(
                    customPrintRequestService.getRequestDetailsForAdmin(id),
                    new CustomPrintOfferFormDto(),
                    new CustomPrintRejectFormDto()
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
                return adminDetailsModelAndView(
                        customPrintRequestService.getRequestDetailsForAdmin(id),
                        offerForm,
                        new CustomPrintRejectFormDto()
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
                return adminDetailsModelAndView(
                        customPrintRequestService.getRequestDetailsForAdmin(id),
                        new CustomPrintOfferFormDto(),
                        rejectForm
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

    private ModelAndView adminDetailsModelAndView(
            CustomPrintRequestDetailsClientDto request,
            CustomPrintOfferFormDto offerForm,
            CustomPrintRejectFormDto rejectForm
    ) {
        ModelAndView modelAndView = new ModelAndView("admin/custom-print-details");
        modelAndView.addObject("request", request);
        modelAndView.addObject("offerForm", offerForm);
        modelAndView.addObject("rejectForm", rejectForm);

        return modelAndView;
    }
}
