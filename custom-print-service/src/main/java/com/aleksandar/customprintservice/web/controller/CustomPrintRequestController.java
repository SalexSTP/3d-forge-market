package com.aleksandar.customprintservice.web.controller;

import com.aleksandar.customprintservice.model.dto.CreateCustomPrintRequestDto;
import com.aleksandar.customprintservice.model.dto.CustomPrintRequestDetailsDto;
import com.aleksandar.customprintservice.model.dto.CustomPrintRequestListItemDto;
import com.aleksandar.customprintservice.model.dto.UpdateCustomPrintQuoteDto;
import com.aleksandar.customprintservice.service.CustomPrintRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/custom-print-requests")
@Tag(name = "Custom Print Requests", description = "REST endpoints for custom 3D print offer requests")
public class CustomPrintRequestController {

    private final CustomPrintRequestService customPrintRequestService;

    @PostMapping
    @Operation(summary = "Create custom print request", description = "Creates a new custom 3D print request with pending review status.")
    @ApiResponse(responseCode = "201", description = "Custom print request created.")
    @ApiResponse(responseCode = "400", description = "Validation failed.")
    public ResponseEntity<CustomPrintRequestDetailsDto> createRequest(
            @Valid @RequestBody CreateCustomPrintRequestDto requestDto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(customPrintRequestService.createRequest(requestDto));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "List customer requests", description = "Returns all custom print requests for one customer ordered by creation date descending.")
    @ApiResponse(responseCode = "200", description = "Customer requests returned.")
    @ApiResponse(responseCode = "400", description = "Invalid customer ID.")
    public ResponseEntity<List<CustomPrintRequestListItemDto>> getCustomerRequests(@PathVariable UUID customerId) {
        return ResponseEntity.ok(customPrintRequestService.getCustomerRequests(customerId));
    }

    @GetMapping("/customer/{customerId}/{requestId}")
    @Operation(summary = "Get customer request details", description = "Returns details for one request owned by the specified customer.")
    @ApiResponse(responseCode = "200", description = "Customer request details returned.")
    @ApiResponse(responseCode = "400", description = "Invalid path parameter.")
    @ApiResponse(responseCode = "404", description = "Custom print request was not found.")
    public ResponseEntity<CustomPrintRequestDetailsDto> getCustomerRequestDetails(
            @PathVariable UUID customerId,
            @PathVariable UUID requestId) {

        return ResponseEntity.ok(customPrintRequestService.getCustomerRequestDetails(requestId, customerId));
    }

    @GetMapping
    @Operation(summary = "List all requests", description = "Returns all custom print requests ordered by creation date descending.")
    @ApiResponse(responseCode = "200", description = "Custom print requests returned.")
    public ResponseEntity<List<CustomPrintRequestListItemDto>> getAllRequests() {
        return ResponseEntity.ok(customPrintRequestService.getAllRequests());
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "Get request details", description = "Returns details for one custom print request.")
    @ApiResponse(responseCode = "200", description = "Custom print request details returned.")
    @ApiResponse(responseCode = "400", description = "Invalid request ID.")
    @ApiResponse(responseCode = "404", description = "Custom print request was not found.")
    public ResponseEntity<CustomPrintRequestDetailsDto> getRequestDetails(@PathVariable UUID requestId) {
        return ResponseEntity.ok(customPrintRequestService.getRequestDetails(requestId));
    }

    @PutMapping("/{requestId}/quote")
    @Operation(summary = "Send offer or reject request", description = "Updates a pending request to OFFER_SENT with offer details or REJECTED with an admin message.")
    @ApiResponse(responseCode = "200", description = "Custom print request response updated.")
    @ApiResponse(responseCode = "400", description = "Validation failed or operation is not allowed.")
    @ApiResponse(responseCode = "404", description = "Custom print request was not found.")
    public ResponseEntity<CustomPrintRequestDetailsDto> updateQuote(
            @PathVariable UUID requestId,
            @Valid @RequestBody UpdateCustomPrintQuoteDto quoteDto) {

        return ResponseEntity.ok(customPrintRequestService.updateQuote(requestId, quoteDto));
    }

    @PutMapping("/customer/{customerId}/{requestId}/cancel")
    @Operation(summary = "Cancel customer request", description = "Cancels a pending or offer-sent request owned by the specified customer.")
    @ApiResponse(responseCode = "200", description = "Custom print request cancelled.")
    @ApiResponse(responseCode = "400", description = "Invalid path parameter or operation is not allowed.")
    @ApiResponse(responseCode = "404", description = "Custom print request was not found.")
    public ResponseEntity<CustomPrintRequestDetailsDto> cancelCustomerRequest(
            @PathVariable UUID customerId,
            @PathVariable UUID requestId) {

        return ResponseEntity.ok(customPrintRequestService.cancelCustomerRequest(requestId, customerId));
    }
}
