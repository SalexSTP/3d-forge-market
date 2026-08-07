package com.aleksandar.threedforgemarket.integration.customprint;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "custom-print-service",
        url = "${custom-print-service.base-url}"
)
public interface CustomPrintRequestClient {

    @PostMapping("/api/custom-print-requests")
    CustomPrintRequestDetailsClientDto createRequest(
            @RequestBody CreateCustomPrintRequestClientDto requestDto
    );

    @GetMapping("/api/custom-print-requests/customer/{customerId}")
    List<CustomPrintRequestListItemClientDto> getCustomerRequests(
            @PathVariable("customerId") UUID customerId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) CustomPrintRequestStatus status,
            @RequestParam(value = "createdFrom", required = false) String createdFrom,
            @RequestParam(value = "createdTo", required = false) String createdTo
    );

    @GetMapping("/api/custom-print-requests/customer/{customerId}/{requestId}")
    CustomPrintRequestDetailsClientDto getCustomerRequestDetails(
            @PathVariable("customerId") UUID customerId,
            @PathVariable("requestId") UUID requestId
    );

    @GetMapping("/api/custom-print-requests")
    List<CustomPrintRequestListItemClientDto> getAllRequests(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) CustomPrintRequestStatus status,
            @RequestParam(value = "createdFrom", required = false) String createdFrom,
            @RequestParam(value = "createdTo", required = false) String createdTo
    );

    @GetMapping("/api/custom-print-requests/{requestId}")
    CustomPrintRequestDetailsClientDto getRequestDetails(
            @PathVariable("requestId") UUID requestId
    );

    @PutMapping("/api/custom-print-requests/{requestId}/offer")
    CustomPrintRequestDetailsClientDto sendOffer(
            @PathVariable("requestId") UUID requestId,
            @RequestBody UpdateCustomPrintOfferClientDto offerDto
    );

    @PutMapping("/api/custom-print-requests/{requestId}/reject")
    CustomPrintRequestDetailsClientDto rejectRequest(
            @PathVariable("requestId") UUID requestId,
            @RequestBody RejectCustomPrintRequestClientDto rejectDto
    );

    @PutMapping("/api/custom-print-requests/{requestId}/fulfillment-status")
    CustomPrintRequestDetailsClientDto updateFulfillmentStatus(
            @PathVariable("requestId") UUID requestId,
            @RequestBody UpdateCustomPrintFulfillmentStatusClientDto statusDto
    );

    @PutMapping("/api/custom-print-requests/customer/{customerId}/{requestId}/accept")
    CustomPrintRequestDetailsClientDto acceptOffer(
            @PathVariable("customerId") UUID customerId,
            @PathVariable("requestId") UUID requestId
    );

    @PutMapping("/api/custom-print-requests/customer/{customerId}/{requestId}/request-changes")
    CustomPrintRequestDetailsClientDto requestChanges(
            @PathVariable("customerId") UUID customerId,
            @PathVariable("requestId") UUID requestId,
            @RequestBody RequestCustomPrintChangesClientDto requestDto
    );

    @PutMapping("/api/custom-print-requests/customer/{customerId}/{requestId}/cancel")
    CustomPrintRequestDetailsClientDto cancelCustomerRequest(
            @PathVariable("customerId") UUID customerId,
            @PathVariable("requestId") UUID requestId
    );

    @PutMapping("/api/custom-print-requests/customer/{customerId}/{requestId}/hide")
    CustomPrintRequestDetailsClientDto hideCustomerRequest(
            @PathVariable("customerId") UUID customerId,
            @PathVariable("requestId") UUID requestId
    );

    @PutMapping("/api/custom-print-requests/{requestId}/archive")
    CustomPrintRequestDetailsClientDto archiveRequest(
            @PathVariable("requestId") UUID requestId
    );
}
