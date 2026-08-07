package com.aleksandar.customprintservice.web.controller;

import com.aleksandar.customprintservice.model.dto.CreateCustomPrintRequestDto;
import com.aleksandar.customprintservice.model.dto.CustomPrintRequestDetailsDto;
import com.aleksandar.customprintservice.model.dto.CustomPrintRequestListItemDto;
import com.aleksandar.customprintservice.model.dto.RejectCustomPrintRequestDto;
import com.aleksandar.customprintservice.model.dto.RequestCustomPrintChangesDto;
import com.aleksandar.customprintservice.model.dto.SendCustomPrintOfferDto;
import com.aleksandar.customprintservice.model.enums.CustomPrintRequestStatus;
import com.aleksandar.customprintservice.service.CustomPrintRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/custom-print-requests")
@Tag(name = "Custom Print Requests", description = "REST endpoints for custom 3D print offer requests")
public class CustomPrintRequestController {

    private final CustomPrintRequestService customPrintRequestService;

    @PostMapping
    @Operation(summary = "Create custom print request", description = "Creates a new custom 3D print request with delivery address and pending review status.")
    @ApiResponse(responseCode = "201", description = "Custom print request created.")
    @ApiResponse(responseCode = "400", description = "Validation failed.")
    public ResponseEntity<CustomPrintRequestDetailsDto> createRequest(
            @Valid @RequestBody CreateCustomPrintRequestDto requestDto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(customPrintRequestService.createRequest(requestDto));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "List customer requests", description = "Returns visible customer requests with optional keyword, status, and created date filters.")
    @ApiResponse(responseCode = "200", description = "Customer requests returned.")
    @ApiResponse(responseCode = "400", description = "Invalid customer ID.")
    public ResponseEntity<List<CustomPrintRequestListItemDto>> getCustomerRequests(
            @PathVariable UUID customerId,
            @Parameter(description = "Searches title, material, color description, and description.")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Filter by request status, including CHANGES_REQUESTED.")
            @RequestParam(required = false) CustomPrintRequestStatus status,
            @Parameter(description = "Created-on lower bound in ISO date-time format.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @Parameter(description = "Created-on upper bound in ISO date-time format.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo) {

        return ResponseEntity.ok(customPrintRequestService.getCustomerRequests(customerId, keyword, status, createdFrom, createdTo));
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
    @Operation(summary = "List all requests", description = "Returns visible admin requests with optional keyword, status, and created date filters.")
    @ApiResponse(responseCode = "200", description = "Custom print requests returned.")
    public ResponseEntity<List<CustomPrintRequestListItemDto>> getAllRequests(
            @Parameter(description = "Searches title, material, color description, description, customer username, and customer email.")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Filter by request status, including CHANGES_REQUESTED.")
            @RequestParam(required = false) CustomPrintRequestStatus status,
            @Parameter(description = "Created-on lower bound in ISO date-time format.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @Parameter(description = "Created-on upper bound in ISO date-time format.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo) {

        return ResponseEntity.ok(customPrintRequestService.getAllRequests(keyword, status, createdFrom, createdTo));
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "Get request details", description = "Returns details for one custom print request.")
    @ApiResponse(responseCode = "200", description = "Custom print request details returned.")
    @ApiResponse(responseCode = "400", description = "Invalid request ID.")
    @ApiResponse(responseCode = "404", description = "Custom print request was not found.")
    public ResponseEntity<CustomPrintRequestDetailsDto> getRequestDetails(@PathVariable UUID requestId) {
        return ResponseEntity.ok(customPrintRequestService.getRequestDetails(requestId));
    }

    @PutMapping("/{requestId}/offer")
    @Operation(summary = "Send custom print offer", description = "Sends or revises an offer with price, print time, optional admin message, and optional response file URL.")
    @ApiResponse(responseCode = "200", description = "Custom print offer sent.")
    @ApiResponse(responseCode = "400", description = "Validation failed or operation is not allowed.")
    @ApiResponse(responseCode = "404", description = "Custom print request was not found.")
    public ResponseEntity<CustomPrintRequestDetailsDto> sendOffer(
            @PathVariable UUID requestId,
            @Valid @RequestBody SendCustomPrintOfferDto offerDto) {

        return ResponseEntity.ok(customPrintRequestService.sendOffer(requestId, offerDto));
    }

    @PutMapping("/{requestId}/reject")
    @Operation(summary = "Reject custom print request", description = "Rejects a pending or change-requested custom print request with an admin message.")
    @ApiResponse(responseCode = "200", description = "Custom print request rejected.")
    @ApiResponse(responseCode = "400", description = "Validation failed or operation is not allowed.")
    @ApiResponse(responseCode = "404", description = "Custom print request was not found.")
    public ResponseEntity<CustomPrintRequestDetailsDto> rejectRequest(
            @PathVariable UUID requestId,
            @Valid @RequestBody RejectCustomPrintRequestDto rejectDto) {

        return ResponseEntity.ok(customPrintRequestService.rejectRequest(requestId, rejectDto));
    }

    @PutMapping("/customer/{customerId}/{requestId}/accept")
    @Operation(summary = "Accept custom print offer", description = "Allows a customer to accept an offer-sent custom print request.")
    @ApiResponse(responseCode = "200", description = "Custom print offer accepted.")
    @ApiResponse(responseCode = "400", description = "Invalid path parameter or operation is not allowed.")
    @ApiResponse(responseCode = "404", description = "Custom print request was not found.")
    public ResponseEntity<CustomPrintRequestDetailsDto> acceptOffer(
            @PathVariable UUID customerId,
            @PathVariable UUID requestId) {

        return ResponseEntity.ok(customPrintRequestService.acceptOffer(requestId, customerId));
    }

    @PutMapping("/customer/{customerId}/{requestId}/request-changes")
    @Operation(summary = "Request custom print offer changes", description = "Allows a customer to request changes with a message before a revised offer is sent.")
    @ApiResponse(responseCode = "200", description = "Custom print changes requested.")
    @ApiResponse(responseCode = "400", description = "Validation failed or operation is not allowed.")
    @ApiResponse(responseCode = "404", description = "Custom print request was not found.")
    public ResponseEntity<CustomPrintRequestDetailsDto> requestChanges(
            @PathVariable UUID customerId,
            @PathVariable UUID requestId,
            @Valid @RequestBody RequestCustomPrintChangesDto changesDto) {

        return ResponseEntity.ok(customPrintRequestService.requestChanges(requestId, customerId, changesDto));
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

    @PutMapping("/customer/{customerId}/{requestId}/hide")
    @Operation(summary = "Hide customer request", description = "Soft-removes a cancelled or rejected request from the customer's list.")
    @ApiResponse(responseCode = "200", description = "Custom print request hidden from customer list.")
    @ApiResponse(responseCode = "400", description = "Invalid path parameter or operation is not allowed.")
    @ApiResponse(responseCode = "404", description = "Custom print request was not found.")
    public ResponseEntity<CustomPrintRequestDetailsDto> hideCustomerRequest(
            @PathVariable UUID customerId,
            @PathVariable UUID requestId) {

        return ResponseEntity.ok(customPrintRequestService.hideCustomerRequest(requestId, customerId));
    }

    @PutMapping("/{requestId}/archive")
    @Operation(summary = "Archive custom print request", description = "Soft-removes a cancelled or rejected request from the admin list.")
    @ApiResponse(responseCode = "200", description = "Custom print request archived for admin.")
    @ApiResponse(responseCode = "400", description = "Invalid path parameter or operation is not allowed.")
    @ApiResponse(responseCode = "404", description = "Custom print request was not found.")
    public ResponseEntity<CustomPrintRequestDetailsDto> archiveRequest(@PathVariable UUID requestId) {
        return ResponseEntity.ok(customPrintRequestService.archiveRequest(requestId));
    }
}
