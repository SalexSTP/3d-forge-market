package com.aleksandar.threedforgemarket.service.customprint;

import com.aleksandar.threedforgemarket.exception.auth.UserNotFoundException;
import com.aleksandar.threedforgemarket.exception.customprint.CustomPrintRequestNotFoundException;
import com.aleksandar.threedforgemarket.exception.customprint.CustomPrintRequestOperationFailedException;
import com.aleksandar.threedforgemarket.exception.customprint.CustomPrintServiceUnavailableException;
import com.aleksandar.threedforgemarket.integration.customprint.CreateCustomPrintRequestClientDto;
import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestClient;
import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestDetailsClientDto;
import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestListItemClientDto;
import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestStatus;
import com.aleksandar.threedforgemarket.integration.customprint.RejectCustomPrintRequestClientDto;
import com.aleksandar.threedforgemarket.integration.customprint.RequestCustomPrintChangesClientDto;
import com.aleksandar.threedforgemarket.integration.customprint.UpdateCustomPrintOfferClientDto;
import com.aleksandar.threedforgemarket.model.dto.customprint.CustomPrintChangeRequestFormDto;
import com.aleksandar.threedforgemarket.model.dto.customprint.CustomPrintOfferFormDto;
import com.aleksandar.threedforgemarket.model.dto.customprint.CustomPrintRejectFormDto;
import com.aleksandar.threedforgemarket.model.dto.customprint.CustomPrintRequestFormDto;
import com.aleksandar.threedforgemarket.model.dto.customprint.CustomPrintSearchRequest;
import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.enums.user.UserRole;
import com.aleksandar.threedforgemarket.repository.user.UserRepository;
import feign.FeignException;
import feign.RetryableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class CustomPrintRequestService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomPrintRequestService.class);
    private static final String INVALID_OPERATION_MESSAGE = "The custom print request could not be updated.";

    private final CustomPrintRequestClient customPrintRequestClient;
    private final UserRepository userRepository;

    public CustomPrintRequestService(
            CustomPrintRequestClient customPrintRequestClient,
            UserRepository userRepository
    ) {
        this.customPrintRequestClient = customPrintRequestClient;
        this.userRepository = userRepository;
    }

    public List<CustomPrintRequestListItemClientDto> getCustomerRequests(
            UUID customerId,
            CustomPrintSearchRequest searchRequest
    ) {
        validateDateRange(searchRequest);

        try {
            return customPrintRequestClient.getCustomerRequests(
                    customerId,
                    keyword(searchRequest),
                    status(searchRequest),
                    createdFrom(searchRequest),
                    createdTo(searchRequest)
            );
        } catch (FeignException exception) {
            throw translateFeignException(exception);
        }
    }

    public CustomPrintRequestDetailsClientDto getCustomerRequestDetails(
            UUID customerId,
            UUID requestId
    ) {
        try {
            return customPrintRequestClient.getCustomerRequestDetails(customerId, requestId);
        } catch (FeignException exception) {
            throw translateFeignException(exception);
        }
    }

    public void createCustomerRequest(
            UUID customerId,
            CustomPrintRequestFormDto formDto
    ) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(UserNotFoundException::new);

        if (customer.getRole() != UserRole.CUSTOMER) {
            throw new CustomPrintRequestOperationFailedException(
                    "Only customers can submit custom print requests."
            );
        }

        CreateCustomPrintRequestClientDto requestDto = new CreateCustomPrintRequestClientDto(
                customer.getId(),
                customer.getUsername(),
                customer.getEmail(),
                formDto.getTitle(),
                formDto.getDescription(),
                formDto.getMaterial(),
                formDto.getColorDescription(),
                formDto.getWidthCm(),
                formDto.getHeightCm(),
                formDto.getDepthCm(),
                formDto.getQuantity(),
                normalizeOptionalText(formDto.getReferenceFileUrl()),
                formDto.getDeliveryAddress().strip()
        );

        try {
            customPrintRequestClient.createRequest(requestDto);
            LOGGER.info("Created custom print request for customer {}", customer.getId());
        } catch (FeignException exception) {
            throw translateFeignException(exception);
        }
    }

    public void cancelCustomerRequest(UUID customerId, UUID requestId) {
        try {
            customPrintRequestClient.cancelCustomerRequest(customerId, requestId);
            LOGGER.info("Cancelled custom print request {} for customer {}", requestId, customerId);
        } catch (FeignException exception) {
            throw translateFeignException(exception);
        }
    }

    public void acceptOffer(UUID customerId, UUID requestId) {
        try {
            customPrintRequestClient.acceptOffer(customerId, requestId);
            LOGGER.info("Accepted custom print offer for request {} by customer {}", requestId, customerId);
        } catch (FeignException exception) {
            throw translateFeignException(exception);
        }
    }

    public void requestChanges(
            UUID customerId,
            UUID requestId,
            CustomPrintChangeRequestFormDto formDto
    ) {
        RequestCustomPrintChangesClientDto requestDto = new RequestCustomPrintChangesClientDto(
                formDto.getCustomerMessage()
        );

        try {
            customPrintRequestClient.requestChanges(customerId, requestId, requestDto);
            LOGGER.info("Requested changes for custom print request {} by customer {}", requestId, customerId);
        } catch (FeignException exception) {
            throw translateFeignException(exception);
        }
    }

    public void hideCustomerRequest(UUID customerId, UUID requestId) {
        try {
            customPrintRequestClient.hideCustomerRequest(customerId, requestId);
            LOGGER.info("Hid custom print request {} for customer {}", requestId, customerId);
        } catch (FeignException exception) {
            throw translateFeignException(exception);
        }
    }

    public List<CustomPrintRequestListItemClientDto> getAllRequestsForAdmin(
            CustomPrintSearchRequest searchRequest
    ) {
        validateDateRange(searchRequest);

        try {
            return customPrintRequestClient.getAllRequests(
                    keyword(searchRequest),
                    status(searchRequest),
                    createdFrom(searchRequest),
                    createdTo(searchRequest)
            );
        } catch (FeignException exception) {
            throw translateFeignException(exception);
        }
    }

    public CustomPrintRequestDetailsClientDto getRequestDetailsForAdmin(UUID requestId) {
        try {
            return customPrintRequestClient.getRequestDetails(requestId);
        } catch (FeignException exception) {
            throw translateFeignException(exception);
        }
    }

    public void sendOffer(
            UUID requestId,
            CustomPrintOfferFormDto formDto
    ) {
        UpdateCustomPrintOfferClientDto offerDto = new UpdateCustomPrintOfferClientDto(
                formDto.getQuotedPrice(),
                formDto.getEstimatedPrintTimeMinutes(),
                normalizeOptionalText(formDto.getAdminMessage()),
                normalizeOptionalText(formDto.getResponseFileUrl())
        );

        try {
            customPrintRequestClient.sendOffer(requestId, offerDto);
            LOGGER.info("Sent custom print offer for request {}", requestId);
        } catch (FeignException exception) {
            throw translateFeignException(exception);
        }
    }

    public void rejectRequest(
            UUID requestId,
            CustomPrintRejectFormDto formDto
    ) {
        RejectCustomPrintRequestClientDto rejectDto = new RejectCustomPrintRequestClientDto(
                formDto.getAdminMessage()
        );

        try {
            customPrintRequestClient.rejectRequest(requestId, rejectDto);
            LOGGER.info("Rejected custom print request {}", requestId);
        } catch (FeignException exception) {
            throw translateFeignException(exception);
        }
    }

    public void archiveRequest(UUID requestId) {
        try {
            customPrintRequestClient.archiveRequest(requestId);
            LOGGER.info("Archived custom print request {}", requestId);
        } catch (FeignException exception) {
            throw translateFeignException(exception);
        }
    }

    private void validateDateRange(CustomPrintSearchRequest searchRequest) {
        if (searchRequest == null
                || searchRequest.getCreatedFrom() == null
                || searchRequest.getCreatedTo() == null) {
            return;
        }

        if (searchRequest.getCreatedFrom().isAfter(searchRequest.getCreatedTo())) {
            throw new CustomPrintRequestOperationFailedException(
                    "Created from date must be before created to date."
            );
        }
    }

    private String keyword(CustomPrintSearchRequest searchRequest) {
        return searchRequest == null ? null : normalizeOptionalText(searchRequest.getKeyword());
    }

    private CustomPrintRequestStatus status(CustomPrintSearchRequest searchRequest) {
        return searchRequest == null ? null : searchRequest.getStatus();
    }

    private String createdFrom(CustomPrintSearchRequest searchRequest) {
        if (searchRequest == null || searchRequest.getCreatedFrom() == null) {
            return null;
        }

        return formatDateTime(searchRequest.getCreatedFrom().atStartOfDay());
    }

    private String createdTo(CustomPrintSearchRequest searchRequest) {
        if (searchRequest == null || searchRequest.getCreatedTo() == null) {
            return null;
        }

        return formatDateTime(searchRequest.getCreatedTo().atTime(23, 59, 59));
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private RuntimeException translateFeignException(FeignException exception) {
        if (exception instanceof RetryableException || exception.status() == -1) {
            return new CustomPrintServiceUnavailableException();
        }

        if (exception.status() == 404) {
            return new CustomPrintRequestNotFoundException();
        }

        if (exception.status() == 400) {
            return new CustomPrintRequestOperationFailedException(INVALID_OPERATION_MESSAGE);
        }

        if (exception.status() >= 500) {
            return new CustomPrintServiceUnavailableException();
        }

        return new CustomPrintRequestOperationFailedException(INVALID_OPERATION_MESSAGE);
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.strip();
    }
}
