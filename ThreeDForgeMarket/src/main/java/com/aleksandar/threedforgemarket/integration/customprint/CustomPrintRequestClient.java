package com.aleksandar.threedforgemarket.integration.customprint;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
            @PathVariable("customerId") UUID customerId
    );

    @GetMapping("/api/custom-print-requests/customer/{customerId}/{requestId}")
    CustomPrintRequestDetailsClientDto getCustomerRequestDetails(
            @PathVariable("customerId") UUID customerId,
            @PathVariable("requestId") UUID requestId
    );

    @GetMapping("/api/custom-print-requests")
    List<CustomPrintRequestListItemClientDto> getAllRequests();

    @GetMapping("/api/custom-print-requests/{requestId}")
    CustomPrintRequestDetailsClientDto getRequestDetails(
            @PathVariable("requestId") UUID requestId
    );

    @PutMapping("/api/custom-print-requests/{requestId}/quote")
    CustomPrintRequestDetailsClientDto updateQuote(
            @PathVariable("requestId") UUID requestId,
            @RequestBody UpdateCustomPrintOfferClientDto offerDto
    );

    @PutMapping("/api/custom-print-requests/customer/{customerId}/{requestId}/cancel")
    CustomPrintRequestDetailsClientDto cancelCustomerRequest(
            @PathVariable("customerId") UUID customerId,
            @PathVariable("requestId") UUID requestId
    );
}
