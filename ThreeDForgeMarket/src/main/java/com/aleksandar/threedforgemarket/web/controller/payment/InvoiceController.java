package com.aleksandar.threedforgemarket.web.controller.payment;

import com.aleksandar.threedforgemarket.exception.payment.PaymentOperationFailedException;
import com.aleksandar.threedforgemarket.model.dto.payment.InvoiceDownloadResult;
import com.aleksandar.threedforgemarket.security.MarketplaceUserDetails;
import com.aleksandar.threedforgemarket.service.payment.InvoiceService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;
import java.util.UUID;

@Controller
public class InvoiceController {
    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping("/payments/{paymentTransactionId}/invoice")
    public Object downloadCustomerInvoice(
            @PathVariable UUID paymentTransactionId,
            @AuthenticationPrincipal MarketplaceUserDetails currentUser,
            RedirectAttributes redirectAttributes
    ) {
        try {
            return toResponse(invoiceService.downloadCustomerInvoice(paymentTransactionId, currentUser.getId()));
        } catch (PaymentOperationFailedException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/orders/my";
        }
    }

    @GetMapping("/admin/payments/{paymentTransactionId}/invoice")
    public Object downloadAdminInvoice(
            @PathVariable UUID paymentTransactionId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            return toResponse(invoiceService.downloadAdminInvoice(paymentTransactionId));
        } catch (PaymentOperationFailedException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/admin/orders";
        }
    }

    private ResponseEntity<byte[]> toResponse(InvoiceDownloadResult result) {
        if (result.type() == InvoiceDownloadResult.Type.REDIRECT) {
            return ResponseEntity.status(302)
                    .location(URI.create(result.redirectUrl()))
                    .build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(result.filename())
                                .build()
                                .toString()
                )
                .body(result.pdfBytes());
    }
}
