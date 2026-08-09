package com.aleksandar.threedforgemarket.web.controller.payment;

import com.aleksandar.threedforgemarket.exception.payment.PaymentOperationFailedException;
import com.aleksandar.threedforgemarket.service.payment.PaymentService;
import com.aleksandar.threedforgemarket.service.payment.StripeWebhookSession;
import com.stripe.exception.SignatureVerificationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments/stripe/webhook")
public class StripeWebhookController {
    private static final String COMPLETED = "checkout.session.completed";
    private static final String EXPIRED = "checkout.session.expired";

    private final PaymentService paymentService;

    public StripeWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signatureHeader
    ) {
        try {
            StripeWebhookSession completedSession = paymentService.verifyWebhookEvent(
                    payload,
                    signatureHeader,
                    COMPLETED
            );

            if (completedSession != null) {
                paymentService.handleCheckoutSessionCompleted(completedSession);
                return ResponseEntity.ok().build();
            }

            StripeWebhookSession expiredSession = paymentService.verifyWebhookEvent(
                    payload,
                    signatureHeader,
                    EXPIRED
            );

            if (expiredSession != null) {
                paymentService.handleCheckoutSessionExpired(expiredSession);
            }

            return ResponseEntity.ok().build();
        } catch (SignatureVerificationException exception) {
            return ResponseEntity.badRequest().build();
        } catch (PaymentOperationFailedException exception) {
            return ResponseEntity.badRequest().build();
        }
    }
}
