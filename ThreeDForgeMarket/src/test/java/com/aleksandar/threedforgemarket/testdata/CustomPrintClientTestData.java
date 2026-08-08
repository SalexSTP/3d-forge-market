package com.aleksandar.threedforgemarket.testdata;

import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestDetailsClientDto;
import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestStatus;
import com.aleksandar.threedforgemarket.model.dto.customprint.CustomPrintChangeRequestFormDto;
import com.aleksandar.threedforgemarket.model.dto.customprint.CustomPrintFulfillmentStatusFormDto;
import com.aleksandar.threedforgemarket.model.dto.customprint.CustomPrintOfferFormDto;
import com.aleksandar.threedforgemarket.model.dto.customprint.CustomPrintRejectFormDto;
import com.aleksandar.threedforgemarket.model.dto.customprint.CustomPrintRequestFormDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class CustomPrintClientTestData {

    private CustomPrintClientTestData() {
    }

    public static CustomPrintRequestFormDto requestForm() {
        CustomPrintRequestFormDto form = new CustomPrintRequestFormDto();
        form.setTitle("Custom bracket");
        form.setDescription("A durable custom printed bracket for a test fixture.");
        form.setMaterial("PLA");
        form.setColorDescription("Matte black");
        form.setWidthCm(new BigDecimal("10.00"));
        form.setHeightCm(new BigDecimal("5.00"));
        form.setDepthCm(new BigDecimal("3.00"));
        form.setQuantity(2);
        form.setReferenceFileUrl("https://example.com/reference.stl");
        form.setDeliveryAddress("123 Test Street");
        return form;
    }

    public static CustomPrintOfferFormDto offerForm() {
        CustomPrintOfferFormDto form = new CustomPrintOfferFormDto();
        form.setQuotedPrice(new BigDecimal("35.00"));
        form.setEstimatedPrintTimeMinutes(180);
        form.setAdminMessage("Ready to print.");
        form.setResponseFileUrl("https://example.com/response.stl");
        return form;
    }

    public static CustomPrintRejectFormDto rejectForm() {
        CustomPrintRejectFormDto form = new CustomPrintRejectFormDto();
        form.setAdminMessage("Cannot print this request.");
        return form;
    }

    public static CustomPrintChangeRequestFormDto changeRequestForm() {
        CustomPrintChangeRequestFormDto form = new CustomPrintChangeRequestFormDto();
        form.setCustomerMessage("Please adjust the dimensions.");
        return form;
    }

    public static CustomPrintFulfillmentStatusFormDto fulfillmentStatusForm(CustomPrintRequestStatus status) {
        CustomPrintFulfillmentStatusFormDto form = new CustomPrintFulfillmentStatusFormDto();
        form.setStatus(status);
        return form;
    }

    public static CustomPrintRequestDetailsClientDto details(UUID requestId, UUID customerId, CustomPrintRequestStatus status) {
        return new CustomPrintRequestDetailsClientDto(
                requestId,
                customerId,
                "customer",
                "customer@example.com",
                "Custom bracket",
                "A durable custom printed bracket for a test fixture.",
                "PLA",
                "Matte black",
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                new BigDecimal("3.00"),
                2,
                "https://example.com/reference.stl",
                "123 Test Street",
                status,
                new BigDecimal("35.00"),
                180,
                "Ready to print.",
                "https://example.com/response.stl",
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
