package com.aleksandar.customprintservice.service;

import com.aleksandar.customprintservice.exception.CustomPrintRequestNotFoundException;
import com.aleksandar.customprintservice.exception.CustomPrintRequestOperationNotAllowedException;
import com.aleksandar.customprintservice.mapper.CustomPrintRequestMapper;
import com.aleksandar.customprintservice.model.dto.CreateCustomPrintRequestDto;
import com.aleksandar.customprintservice.model.dto.CustomPrintRequestDetailsDto;
import com.aleksandar.customprintservice.model.dto.CustomPrintRequestListItemDto;
import com.aleksandar.customprintservice.model.dto.UpdateCustomPrintQuoteDto;
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
    public List<CustomPrintRequestListItemDto> getCustomerRequests(UUID customerId) {
        return customPrintRequestRepository.findAllByCustomerIdOrderByCreatedOnDesc(customerId)
                .stream()
                .map(customPrintRequestMapper::toListItemDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CustomPrintRequestListItemDto> getAllRequests() {
        return customPrintRequestRepository.findAllByOrderByCreatedOnDesc()
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
    public CustomPrintRequestDetailsDto updateQuote(UUID requestId, UpdateCustomPrintQuoteDto quoteDto) {
        CustomPrintRequest request = findRequestById(requestId);

        if (request.getStatus() != CustomPrintRequestStatus.PENDING_REVIEW) {
            throw new CustomPrintRequestOperationNotAllowedException("Only pending custom print requests can receive an offer or be rejected.");
        }

        if (quoteDto.status() != CustomPrintRequestStatus.OFFER_SENT && quoteDto.status() != CustomPrintRequestStatus.REJECTED) {
            throw new CustomPrintRequestOperationNotAllowedException("Custom print request status can only be changed to offer sent or rejected.");
        }

        if (quoteDto.status() == CustomPrintRequestStatus.REJECTED && isBlank(quoteDto.adminMessage())) {
            throw new CustomPrintRequestOperationNotAllowedException("Admin message is required when rejecting a custom print request.");
        }

        if (quoteDto.status() == CustomPrintRequestStatus.OFFER_SENT) {
            if (quoteDto.quotedPrice() == null) {
                throw new CustomPrintRequestOperationNotAllowedException("Offer price is required when sending an offer for a custom print request.");
            }

            if (quoteDto.estimatedPrintTimeMinutes() == null) {
                throw new CustomPrintRequestOperationNotAllowedException("Estimated print time is required when sending an offer for a custom print request.");
            }

            request.setStatus(CustomPrintRequestStatus.OFFER_SENT);
            request.setQuotedPrice(quoteDto.quotedPrice());
            request.setEstimatedPrintTimeMinutes(quoteDto.estimatedPrintTimeMinutes());
            request.setAdminMessage(quoteDto.adminMessage());
            request.setQuotedOn(LocalDateTime.now());
        } else {
            request.setStatus(CustomPrintRequestStatus.REJECTED);
            request.setQuotedPrice(null);
            request.setEstimatedPrintTimeMinutes(null);
            request.setAdminMessage(quoteDto.adminMessage());
        }

        log.info("Updated custom print request offer with status {}", request.getStatus());

        CustomPrintRequest savedRequest = customPrintRequestRepository.saveAndFlush(request);

        return customPrintRequestMapper.toDetailsDto(savedRequest);
    }

    @Transactional
    public CustomPrintRequestDetailsDto cancelCustomerRequest(UUID requestId, UUID customerId) {
        CustomPrintRequest request = customPrintRequestRepository.findByIdAndCustomerId(requestId, customerId)
                .orElseThrow(() -> new CustomPrintRequestNotFoundException(REQUEST_NOT_FOUND_MESSAGE));

        if (request.getStatus() != CustomPrintRequestStatus.PENDING_REVIEW && request.getStatus() != CustomPrintRequestStatus.OFFER_SENT) {
            throw new CustomPrintRequestOperationNotAllowedException("Only pending or offer-sent custom print requests can be cancelled.");
        }

        request.setStatus(CustomPrintRequestStatus.CANCELLED);
        request.setCancelledOn(LocalDateTime.now());

        log.info("Cancelled custom print request");

        return customPrintRequestMapper.toDetailsDto(request);
    }

    private CustomPrintRequest findRequestById(UUID requestId) {
        return customPrintRequestRepository.findById(requestId)
                .orElseThrow(() -> new CustomPrintRequestNotFoundException(REQUEST_NOT_FOUND_MESSAGE));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
