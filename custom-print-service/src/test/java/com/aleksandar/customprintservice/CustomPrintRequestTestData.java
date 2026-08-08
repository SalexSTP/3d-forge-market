package com.aleksandar.customprintservice;

import com.aleksandar.customprintservice.model.dto.CreateCustomPrintRequestDto;
import com.aleksandar.customprintservice.model.dto.RejectCustomPrintRequestDto;
import com.aleksandar.customprintservice.model.dto.RequestCustomPrintChangesDto;
import com.aleksandar.customprintservice.model.dto.SendCustomPrintOfferDto;
import com.aleksandar.customprintservice.model.dto.UpdateCustomPrintFulfillmentStatusDto;
import com.aleksandar.customprintservice.model.dto.UpdateCustomPrintRequestDto;
import com.aleksandar.customprintservice.model.entity.CustomPrintRequest;
import com.aleksandar.customprintservice.model.enums.CustomPrintRequestStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class CustomPrintRequestTestData {

    public static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID OTHER_CUSTOMER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private CustomPrintRequestTestData() {
    }

    public static CreateCustomPrintRequestDto validCreateDto() {
        return new CreateCustomPrintRequestDto(
                CUSTOMER_ID,
                "aleksandar",
                "aleksandar@example.com",
                "Custom phone stand",
                "Please print a sturdy adjustable phone stand for my desk.",
                "PLA",
                "Matte black",
                "1 Test Street, Sofia",
                new BigDecimal("12.50"),
                new BigDecimal("8.00"),
                new BigDecimal("6.25"),
                2,
                "https://example.com/reference.stl"
        );
    }

    public static UpdateCustomPrintRequestDto validUpdateDto() {
        return new UpdateCustomPrintRequestDto(
                "Updated phone stand",
                "Please print the updated version with a wider base and cable slot.",
                "PETG",
                "White",
                "2 Updated Street, Sofia",
                new BigDecimal("13.00"),
                new BigDecimal("9.00"),
                new BigDecimal("7.00"),
                3,
                "https://example.com/updated-reference.stl"
        );
    }

    public static SendCustomPrintOfferDto validOfferDto() {
        return new SendCustomPrintOfferDto(
                new BigDecimal("49.99"),
                360,
                "We can print this with reinforced walls.",
                "https://example.com/response.stl"
        );
    }

    public static RejectCustomPrintRequestDto validRejectDto() {
        return new RejectCustomPrintRequestDto("The model is not printable with the requested dimensions.");
    }

    public static RequestCustomPrintChangesDto validChangesDto() {
        return new RequestCustomPrintChangesDto("Please reduce the quoted price by using less infill.");
    }

    public static UpdateCustomPrintFulfillmentStatusDto fulfillmentDto(CustomPrintRequestStatus status) {
        return new UpdateCustomPrintFulfillmentStatusDto(status);
    }

    public static CustomPrintRequest validEntity(CustomPrintRequestStatus status) {
        CustomPrintRequest request = new CustomPrintRequest();
        request.setCustomerId(CUSTOMER_ID);
        request.setCustomerUsername("aleksandar");
        request.setCustomerEmail("aleksandar@example.com");
        request.setTitle("Custom phone stand");
        request.setDescription("Please print a sturdy adjustable phone stand for my desk.");
        request.setMaterial("PLA");
        request.setColorDescription("Matte black");
        request.setDeliveryAddress("1 Test Street, Sofia");
        request.setWidthCm(new BigDecimal("12.50"));
        request.setHeightCm(new BigDecimal("8.00"));
        request.setDepthCm(new BigDecimal("6.25"));
        request.setQuantity(2);
        request.setReferenceFileUrl("https://example.com/reference.stl");
        request.setStatus(status);
        return request;
    }

    public static CustomPrintRequest mappedEntityWithMaintenanceFields() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 8, 10, 0);
        CustomPrintRequest request = validEntity(CustomPrintRequestStatus.OFFER_SENT);
        request.setId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        request.setResponseFileUrl("https://example.com/response.stl");
        request.setQuotedPrice(new BigDecimal("49.99"));
        request.setEstimatedPrintTimeMinutes(360);
        request.setAdminMessage("We can print this.");
        request.setCustomerMessage("Looks good.");
        request.setCreatedOn(now.minusDays(5));
        request.setUpdatedOn(now.minusDays(1));
        request.setQuotedOn(now.minusDays(4));
        request.setCustomerRespondedOn(now.minusDays(3));
        request.setAcceptedOn(now.minusDays(2));
        request.setPrintingStartedOn(now.minusDays(1));
        request.setReadyForDeliveryOn(now.minusHours(12));
        request.setDeliveredOn(now.minusHours(6));
        request.setCancelledOn(now.minusHours(5));
        request.setAdminAttentionRequired(true);
        request.setAdminAttentionMarkedOn(now.minusHours(4));
        request.setCustomerResponseReminderRequired(true);
        request.setCustomerResponseReminderMarkedOn(now.minusHours(3));
        request.setAutoArchivedOn(now.minusHours(2));
        return request;
    }
}
