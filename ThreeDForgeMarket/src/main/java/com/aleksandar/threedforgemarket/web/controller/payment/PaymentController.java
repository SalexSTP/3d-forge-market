package com.aleksandar.threedforgemarket.web.controller.payment;

import com.aleksandar.threedforgemarket.exception.customprint.CustomPrintRequestNotFoundException;
import com.aleksandar.threedforgemarket.exception.customprint.CustomPrintRequestOperationFailedException;
import com.aleksandar.threedforgemarket.exception.customprint.CustomPrintServiceUnavailableException;
import com.aleksandar.threedforgemarket.exception.payment.PaymentOperationFailedException;
import com.aleksandar.threedforgemarket.exception.payment.StripePaymentUnavailableException;
import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestDetailsClientDto;
import com.aleksandar.threedforgemarket.model.dto.payment.PaymentMethodFormDto;
import com.aleksandar.threedforgemarket.model.dto.payment.PaymentStartResult;
import com.aleksandar.threedforgemarket.model.dto.payment.StripeCancelResult;
import com.aleksandar.threedforgemarket.model.dto.payment.StripeSuccessResult;
import com.aleksandar.threedforgemarket.model.enums.payment.PaymentMethod;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;
    private final CustomPrintRequestService customPrintRequestService;

    public PaymentController(
            PaymentService paymentService,
            CustomPrintRequestService customPrintRequestService
    ) {
        this.paymentService = paymentService;
        this.customPrintRequestService = customPrintRequestService;
    }

    @GetMapping("/custom-prints/{requestId}")
    public ModelAndView getCustomPrintPaymentPage(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal MarketplaceUserDetails currentUser,
            RedirectAttributes redirectAttributes
    ) {
        try {
            CustomPrintRequestDetailsClientDto request = customPrintRequestService.getCustomerRequestDetails(
                    currentUser.getId(),
                    requestId
            );
            validateOfferForPayment(request);

            PaymentMethodFormDto paymentForm = new PaymentMethodFormDto();
            paymentForm.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);

            return customPrintPaymentModelAndView(request, paymentForm);

        } catch (CustomPrintRequestNotFoundException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "That custom print request could not be found.");
            return new ModelAndView("redirect:/custom-prints");
        } catch (CustomPrintRequestOperationFailedException
                 | CustomPrintServiceUnavailableException
                 | PaymentOperationFailedException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return new ModelAndView("redirect:/custom-prints/" + requestId);
        }
    }

    @PostMapping("/custom-prints/{requestId}")
    public ModelAndView startCustomPrintPayment(
            @PathVariable UUID requestId,
            @Valid @ModelAttribute("paymentForm") PaymentMethodFormDto paymentForm,
            BindingResult bindingResult,
            @AuthenticationPrincipal MarketplaceUserDetails currentUser,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            try {
                CustomPrintRequestDetailsClientDto request = customPrintRequestService.getCustomerRequestDetails(
                        currentUser.getId(),
                        requestId
                );
                validateOfferForPayment(request);
                return customPrintPaymentModelAndView(request, paymentForm);
            } catch (CustomPrintRequestOperationFailedException
                     | CustomPrintServiceUnavailableException
                     | PaymentOperationFailedException exception) {
                redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
                return new ModelAndView("redirect:/custom-prints/" + requestId);
            }
        }

        try {
            PaymentStartResult paymentStartResult = paymentService.startCustomPrintOfferPayment(
                    currentUser.getId(),
                    requestId,
                    paymentForm.getPaymentMethod()
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    paymentForm.getPaymentMethod() == PaymentMethod.STRIPE_CHECKOUT
                            ? "Complete payment in Stripe Checkout to accept this offer."
                            : "The custom print offer was accepted."
            );

            return new ModelAndView("redirect:" + paymentStartResult.redirectUrl());

        } catch (CustomPrintRequestNotFoundException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "That custom print request could not be found.");
            return new ModelAndView("redirect:/custom-prints");
        } catch (CustomPrintRequestOperationFailedException
                 | CustomPrintServiceUnavailableException
                 | PaymentOperationFailedException
                 | StripePaymentUnavailableException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return new ModelAndView("redirect:/custom-prints/" + requestId);
        }
    }

    @GetMapping("/stripe/success")
    public ModelAndView stripeSuccess(
            @RequestParam(name = "session_id", required = false) String sessionId,
            @AuthenticationPrincipal MarketplaceUserDetails currentUser,
            RedirectAttributes redirectAttributes
    ) {
        try {
            StripeSuccessResult successResult = paymentService.resolveStripeCheckoutSuccess(
                    sessionId,
                    currentUser.getId()
            );

            if (successResult.targetType() == PaymentTargetType.PRODUCT_ORDER) {
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Payment completed. Your order is now waiting for admin review."
                );
                return new ModelAndView("redirect:/orders");
            }

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Payment completed. Your custom print offer will be accepted after Stripe confirms the payment."
            );
            return new ModelAndView("redirect:/custom-prints/" + successResult.targetId());

        } catch (PaymentOperationFailedException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return new ModelAndView("redirect:/orders");
        }
    }

    @GetMapping("/stripe/cancel")
    public ModelAndView stripeCancel(
            @RequestParam(name = "paymentTransactionId", required = false) UUID paymentTransactionId,
            @AuthenticationPrincipal MarketplaceUserDetails currentUser,
            RedirectAttributes redirectAttributes
    ) {
        if (paymentTransactionId == null) {
            return new ModelAndView("payment/cancel");
        }

        try {
            StripeCancelResult cancelResult = paymentService.cancelStripeCheckoutPayment(
                    paymentTransactionId,
                    currentUser.getId()
            );

            redirectAttributes.addFlashAttribute(
                    "infoMessage",
                    "Online payment was cancelled. You can review your order and try again."
            );

            if (cancelResult.targetType() == PaymentTargetType.PRODUCT_ORDER) {
                redirectAttributes.addFlashAttribute("orderForm", cancelResult.orderForm());
                return new ModelAndView("redirect:/orders/create?productId=" + cancelResult.productId());
            }

            return new ModelAndView("redirect:/payments/custom-prints/" + cancelResult.customPrintRequestId());

        } catch (PaymentOperationFailedException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return new ModelAndView("redirect:/orders/my");
        }
    }

    private ModelAndView customPrintPaymentModelAndView(
            CustomPrintRequestDetailsClientDto request,
            PaymentMethodFormDto paymentForm
    ) {
        ModelAndView modelAndView = new ModelAndView("payment/custom-print");
        modelAndView.addObject("request", request);
        modelAndView.addObject("paymentForm", paymentForm);
        modelAndView.addObject("paymentMethods", PaymentMethod.values());
        modelAndView.addObject("stripeAvailable", paymentService.isStripeCheckoutAvailable());
        modelAndView.addObject("paymentSummary", paymentService
                .getLatestPaymentSummary(PaymentTargetType.CUSTOM_PRINT_REQUEST, request.id())
                .orElse(null));

        return modelAndView;
    }

    private void validateOfferForPayment(CustomPrintRequestDetailsClientDto request) {
        if (!request.isOfferSent()) {
            throw new PaymentOperationFailedException("Only offers waiting for your response can be paid.");
        }

        if (request.quotedPrice() == null || request.quotedPrice().signum() <= 0) {
            throw new PaymentOperationFailedException("This payment can no longer be completed.");
        }
    }
}
