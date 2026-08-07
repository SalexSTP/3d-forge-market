package com.aleksandar.customprintservice.service;

import com.aleksandar.customprintservice.exception.CustomPrintRequestNotFoundException;
import com.aleksandar.customprintservice.exception.CustomPrintRequestOperationNotAllowedException;
import com.aleksandar.customprintservice.mapper.CustomPrintRequestMapper;
import com.aleksandar.customprintservice.model.dto.CreateCustomPrintRequestDto;
import com.aleksandar.customprintservice.model.dto.CustomPrintRequestDetailsDto;
import com.aleksandar.customprintservice.model.dto.CustomPrintRequestListItemDto;
import com.aleksandar.customprintservice.model.dto.RejectCustomPrintRequestDto;
import com.aleksandar.customprintservice.model.dto.RequestCustomPrintChangesDto;
import com.aleksandar.customprintservice.model.dto.SendCustomPrintOfferDto;
import com.aleksandar.customprintservice.model.dto.UpdateCustomPrintFulfillmentStatusDto;
import com.aleksandar.customprintservice.model.entity.CustomPrintRequest;
import com.aleksandar.customprintservice.model.enums.CustomPrintRequestStatus;
import com.aleksandar.customprintservice.repository.CustomPrintRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomPrintRequestService {

    private static final String REQUEST_NOT_FOUND_MESSAGE = "Custom print request was not found.";

    private final CustomPrintRequestRepository customPrintRequestRepository;
    private final CustomPrintRequestMapper customPrintRequestMapper;

    @Transactional
    public CustomPrintRequestDetailsDto createRequest(CreateCustomPrintRequestDto requestDto) {
        CustomPrintRequest request = customPrintRequestMapper.toEntity(requestDto);
        CustomPrintRequest savedRequest = customPrintRequestRepository.save(request);

        log.info("Created custom print request with status {}", savedRequest.getStatus());

        return customPrintRequestMapper.toDetailsDto(savedRequest);
    }

    @Transactional(readOnly = true)
    public List<CustomPrintRequestListItemDto> getCustomerRequests(
            UUID customerId,
            String keyword,
            CustomPrintRequestStatus status,
            LocalDateTime createdFrom,
            LocalDateTime createdTo) {

        return customPrintRequestRepository.searchCustomerRequests(customerId, normalizeKeyword(keyword), status, createdFrom, createdTo)
                .stream()
                .map(customPrintRequestMapper::toListItemDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CustomPrintRequestListItemDto> getAllRequests(
            String keyword,
            CustomPrintRequestStatus status,
            LocalDateTime createdFrom,
            LocalDateTime createdTo) {

        return customPrintRequestRepository.searchAdminRequests(normalizeKeyword(keyword), status, createdFrom, createdTo)
                .stream()
                .map(customPrintRequestMapper::toListItemDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomPrintRequestDetailsDto getRequestDetails(UUID requestId) {
        return customPrintRequestMapper.toDetailsDto(findRequestById(requestId));
    }

    @Transactional(readOnly = true)
    public CustomPrintRequestDetailsDto getCustomerRequestDetails(UUID requestId, UUID customerId) {
        CustomPrintRequest request = customPrintRequestRepository.findByIdAndCustomerId(requestId, customerId)
                .orElseThrow(() -> new CustomPrintRequestNotFoundException(REQUEST_NOT_FOUND_MESSAGE));

        return customPrintRequestMapper.toDetailsDto(request);
    }

    @Transactional
    public CustomPrintRequestDetailsDto sendOffer(UUID requestId, SendCustomPrintOfferDto offerDto) {
        CustomPrintRequest request = findRequestById(requestId);

        if (request.getStatus() != CustomPrintRequestStatus.PENDING_REVIEW
                && request.getStatus() != CustomPrintRequestStatus.CHANGES_REQUESTED) {
            throw new CustomPrintRequestOperationNotAllowedException("Only pending or change-requested custom print requests can receive an offer.");
        }

        request.setStatus(CustomPrintRequestStatus.OFFER_SENT);
        request.setQuotedPrice(offerDto.quotedPrice());
        request.setEstimatedPrintTimeMinutes(offerDto.estimatedPrintTimeMinutes());
        request.setAdminMessage(offerDto.adminMessage());
        request.setResponseFileUrl(offerDto.responseFileUrl());
        request.setQuotedOn(LocalDateTime.now());

        log.info("Sent custom print request offer");

        CustomPrintRequest savedRequest = customPrintRequestRepository.saveAndFlush(request);

        return customPrintRequestMapper.toDetailsDto(savedRequest);
    }

    @Transactional
    public CustomPrintRequestDetailsDto rejectRequest(UUID requestId, RejectCustomPrintRequestDto rejectDto) {
        CustomPrintRequest request = findRequestById(requestId);

        if (request.getStatus() != CustomPrintRequestStatus.PENDING_REVIEW
                && request.getStatus() != CustomPrintRequestStatus.CHANGES_REQUESTED) {
            throw new CustomPrintRequestOperationNotAllowedException("Only pending or change-requested custom print requests can be rejected.");
        }

        request.setStatus(CustomPrintRequestStatus.REJECTED);
        request.setQuotedPrice(null);
        request.setEstimatedPrintTimeMinutes(null);
        request.setAdminMessage(rejectDto.adminMessage());

        log.info("Rejected custom print request");

        CustomPrintRequest savedRequest = customPrintRequestRepository.saveAndFlush(request);

        return customPrintRequestMapper.toDetailsDto(savedRequest);
    }

    @Transactional
    public CustomPrintRequestDetailsDto acceptOffer(UUID requestId, UUID customerId) {
        CustomPrintRequest request = findCustomerRequestById(requestId, customerId);

        if (request.getStatus() != CustomPrintRequestStatus.OFFER_SENT) {
            throw new CustomPrintRequestOperationNotAllowedException("Only offer-sent custom print requests can be accepted.");
        }

        request.setStatus(CustomPrintRequestStatus.ACCEPTED);
        request.setAcceptedOn(LocalDateTime.now());

        log.info("Accepted custom print request offer");

        return customPrintRequestMapper.toDetailsDto(request);
    }

    @Transactional
    public CustomPrintRequestDetailsDto requestChanges(UUID requestId, UUID customerId, RequestCustomPrintChangesDto changesDto) {
        CustomPrintRequest request = findCustomerRequestById(requestId, customerId);

        if (request.getStatus() != CustomPrintRequestStatus.OFFER_SENT) {
            throw new CustomPrintRequestOperationNotAllowedException("Only offer-sent custom print requests can receive customer change requests.");
        }

        request.setStatus(CustomPrintRequestStatus.CHANGES_REQUESTED);
        request.setCustomerMessage(changesDto.customerMessage());
        request.setCustomerRespondedOn(LocalDateTime.now());

        log.info("Requested changes for custom print request");

        return customPrintRequestMapper.toDetailsDto(request);
    }

    @Transactional
    public CustomPrintRequestDetailsDto cancelCustomerRequest(UUID requestId, UUID customerId) {
        CustomPrintRequest request = findCustomerRequestById(requestId, customerId);

        if (request.getStatus() != CustomPrintRequestStatus.PENDING_REVIEW
                && request.getStatus() != CustomPrintRequestStatus.OFFER_SENT
                && request.getStatus() != CustomPrintRequestStatus.CHANGES_REQUESTED) {
            throw new CustomPrintRequestOperationNotAllowedException("Only pending, offer-sent, or change-requested custom print requests can be cancelled.");
        }

        request.setStatus(CustomPrintRequestStatus.CANCELLED);
        request.setCancelledOn(LocalDateTime.now());

        log.info("Cancelled custom print request");

        return customPrintRequestMapper.toDetailsDto(request);
    }

    @Transactional
    public CustomPrintRequestDetailsDto updateFulfillmentStatus(UUID requestId, UpdateCustomPrintFulfillmentStatusDto statusDto) {
        CustomPrintRequest request = findRequestById(requestId);
        CustomPrintRequestStatus nextStatus = statusDto.status();

        if (request.getStatus() == CustomPrintRequestStatus.ACCEPTED && nextStatus == CustomPrintRequestStatus.PRINTING) {
            request.setStatus(CustomPrintRequestStatus.PRINTING);
            request.setPrintingStartedOn(LocalDateTime.now());
        } else if (request.getStatus() == CustomPrintRequestStatus.PRINTING && nextStatus == CustomPrintRequestStatus.READY_FOR_DELIVERY) {
            request.setStatus(CustomPrintRequestStatus.READY_FOR_DELIVERY);
            request.setReadyForDeliveryOn(LocalDateTime.now());
        } else if (request.getStatus() == CustomPrintRequestStatus.READY_FOR_DELIVERY && nextStatus == CustomPrintRequestStatus.DELIVERED) {
            request.setStatus(CustomPrintRequestStatus.DELIVERED);
            request.setDeliveredOn(LocalDateTime.now());
        } else {
            throw new CustomPrintRequestOperationNotAllowedException("Custom print fulfillment status can only move to the next production step.");
        }

        log.info("Updated custom print fulfillment status to {}", request.getStatus());

        return customPrintRequestMapper.toDetailsDto(request);
    }

    @Transactional
    public CustomPrintRequestDetailsDto hideCustomerRequest(UUID requestId, UUID customerId) {
        CustomPrintRequest request = findCustomerRequestById(requestId, customerId);

        requireRemovableFromList(request);

        request.setHiddenFromCustomer(true);
        request.setHiddenFromCustomerOn(LocalDateTime.now());

        log.info("Customer hid custom print request");

        return customPrintRequestMapper.toDetailsDto(request);
    }

    @Transactional
    public CustomPrintRequestDetailsDto archiveRequest(UUID requestId) {
        CustomPrintRequest request = findRequestById(requestId);

        requireRemovableFromList(request);

        request.setHiddenFromAdmin(true);
        request.setHiddenFromAdminOn(LocalDateTime.now());

        log.info("Archived custom print request");

        return customPrintRequestMapper.toDetailsDto(request);
    }

    private CustomPrintRequest findRequestById(UUID requestId) {
        return customPrintRequestRepository.findById(requestId)
                .orElseThrow(() -> new CustomPrintRequestNotFoundException(REQUEST_NOT_FOUND_MESSAGE));
    }

    private CustomPrintRequest findCustomerRequestById(UUID requestId, UUID customerId) {
        return customPrintRequestRepository.findByIdAndCustomerId(requestId, customerId)
                .orElseThrow(() -> new CustomPrintRequestNotFoundException(REQUEST_NOT_FOUND_MESSAGE));
    }

    private void requireRemovableFromList(CustomPrintRequest request) {
        if (request.getStatus() != CustomPrintRequestStatus.CANCELLED
                && request.getStatus() != CustomPrintRequestStatus.REJECTED
                && request.getStatus() != CustomPrintRequestStatus.DELIVERED) {
            throw new CustomPrintRequestOperationNotAllowedException("Only cancelled, rejected, or delivered custom print requests can be removed from lists.");
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return keyword.trim();
    }
}
