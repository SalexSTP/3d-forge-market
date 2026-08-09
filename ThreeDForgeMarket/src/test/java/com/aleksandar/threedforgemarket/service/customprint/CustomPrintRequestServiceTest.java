package com.aleksandar.threedforgemarket.service.customprint;

import com.aleksandar.threedforgemarket.exception.customprint.CustomPrintRequestNotFoundException;
import com.aleksandar.threedforgemarket.exception.customprint.CustomPrintRequestOperationFailedException;
import com.aleksandar.threedforgemarket.exception.customprint.CustomPrintServiceUnavailableException;
import com.aleksandar.threedforgemarket.integration.customprint.CreateCustomPrintRequestClientDto;
import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestClient;
import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestStatus;
import com.aleksandar.threedforgemarket.integration.customprint.RequestCustomPrintChangesClientDto;
import com.aleksandar.threedforgemarket.integration.customprint.UpdateCustomPrintFulfillmentStatusClientDto;
import com.aleksandar.threedforgemarket.integration.customprint.UpdateCustomPrintOfferClientDto;
import com.aleksandar.threedforgemarket.integration.customprint.UpdateCustomPrintRequestClientDto;
import com.aleksandar.threedforgemarket.model.dto.customprint.CustomPrintRequestFormDto;
import com.aleksandar.threedforgemarket.model.dto.customprint.CustomPrintSearchRequest;
import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.repository.user.UserRepository;
import com.aleksandar.threedforgemarket.testdata.CustomPrintClientTestData;
import com.aleksandar.threedforgemarket.testdata.UserTestData;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class CustomPrintRequestServiceTest {

    @Mock
    private CustomPrintRequestClient customPrintRequestClient;

    @Mock
    private UserRepository userRepository;

    private CustomPrintRequestService customPrintRequestService;
    private UUID customerId;
    private UUID requestId;

    @BeforeEach
    void setUp() {
        customPrintRequestService = new CustomPrintRequestService(customPrintRequestClient, userRepository);
        customerId = UUID.randomUUID();
        requestId = UUID.randomUUID();
    }

    @Test
    void createRequestSendsCustomerIdentityFromServerSideUserData() {
        User customer = UserTestData.customer("customer");
        customer.setId(customerId);
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customPrintRequestClient.createRequest(any()))
                .thenReturn(CustomPrintClientTestData.details(requestId, customerId, CustomPrintRequestStatus.PENDING_REVIEW));

        customPrintRequestService.createCustomerRequest(customerId, CustomPrintClientTestData.requestForm());

        ArgumentCaptor<CreateCustomPrintRequestClientDto> captor = ArgumentCaptor.forClass(CreateCustomPrintRequestClientDto.class);
        verify(customPrintRequestClient).createRequest(captor.capture());
        assertThat(captor.getValue().customerId()).isEqualTo(customerId);
        assertThat(captor.getValue().customerUsername()).isEqualTo("customer");
        assertThat(captor.getValue().customerEmail()).isEqualTo("customer@example.com");
    }

    @Test
    void editRequestMapsFormToUpdateClientDto() {
        CustomPrintRequestFormDto form = CustomPrintClientTestData.requestForm();

        customPrintRequestService.updateCustomerRequest(customerId, requestId, form);

        ArgumentCaptor<UpdateCustomPrintRequestClientDto> captor = ArgumentCaptor.forClass(UpdateCustomPrintRequestClientDto.class);
        verify(customPrintRequestClient).updateCustomerRequest(eq(customerId), eq(requestId), captor.capture());
        assertThat(captor.getValue().title()).isEqualTo(form.getTitle());
        assertThat(captor.getValue().deliveryAddress()).isEqualTo(form.getDeliveryAddress());
        assertThat(captor.getValue().referenceFileUrl()).isEqualTo(form.getReferenceFileUrl());
    }

    @Test
    void getEditFormAllowedOnlyForPendingReview() {
        when(customPrintRequestClient.getCustomerRequestDetails(customerId, requestId))
                .thenReturn(CustomPrintClientTestData.details(requestId, customerId, CustomPrintRequestStatus.PENDING_REVIEW));

        CustomPrintRequestFormDto form = customPrintRequestService.getEditForm(customerId, requestId);

        assertThat(form.getTitle()).isEqualTo("Custom bracket");
    }

    @Test
    void getEditFormBlockedForOfferSentAndOtherStatuses() {
        when(customPrintRequestClient.getCustomerRequestDetails(customerId, requestId))
                .thenReturn(CustomPrintClientTestData.details(requestId, customerId, CustomPrintRequestStatus.OFFER_SENT));

        assertThatThrownBy(() -> customPrintRequestService.getEditForm(customerId, requestId))
                .isInstanceOf(CustomPrintRequestOperationFailedException.class);

        when(customPrintRequestClient.getCustomerRequestDetails(customerId, requestId))
                .thenReturn(CustomPrintClientTestData.details(requestId, customerId, CustomPrintRequestStatus.ACCEPTED));

        assertThatThrownBy(() -> customPrintRequestService.getEditForm(customerId, requestId))
                .isInstanceOf(CustomPrintRequestOperationFailedException.class);
    }

    @Test
    void customerActionsCallFeignClient() {
        customPrintRequestService.cancelCustomerRequest(customerId, requestId);
        customPrintRequestService.acceptOffer(customerId, requestId);
        customPrintRequestService.requestChanges(customerId, requestId, CustomPrintClientTestData.changeRequestForm());

        verify(customPrintRequestClient).cancelCustomerRequest(customerId, requestId);
        verify(customPrintRequestClient).acceptOffer(customerId, requestId);
        ArgumentCaptor<RequestCustomPrintChangesClientDto> captor = ArgumentCaptor.forClass(RequestCustomPrintChangesClientDto.class);
        verify(customPrintRequestClient).requestChanges(eq(customerId), eq(requestId), captor.capture());
        assertThat(captor.getValue().customerMessage()).isEqualTo("Please adjust the dimensions.");
    }

    @Test
    void adminActionsCallFeignClient() {
        customPrintRequestService.sendOffer(requestId, CustomPrintClientTestData.offerForm());
        customPrintRequestService.rejectRequest(requestId, CustomPrintClientTestData.rejectForm());
        customPrintRequestService.updateFulfillmentStatus(
                requestId,
                CustomPrintClientTestData.fulfillmentStatusForm(CustomPrintRequestStatus.PRINTING)
        );

        ArgumentCaptor<UpdateCustomPrintOfferClientDto> offerCaptor = ArgumentCaptor.forClass(UpdateCustomPrintOfferClientDto.class);
        verify(customPrintRequestClient).sendOffer(eq(requestId), offerCaptor.capture());
        assertThat(offerCaptor.getValue().quotedPrice()).isEqualByComparingTo("35.00");
        verify(customPrintRequestClient).rejectRequest(eq(requestId), any());
        ArgumentCaptor<UpdateCustomPrintFulfillmentStatusClientDto> statusCaptor = ArgumentCaptor.forClass(UpdateCustomPrintFulfillmentStatusClientDto.class);
        verify(customPrintRequestClient).updateFulfillmentStatus(eq(requestId), statusCaptor.capture());
        assertThat(statusCaptor.getValue().status()).isEqualTo(CustomPrintRequestStatus.PRINTING);
    }

    @Test
    void feignErrorsTranslateToFriendlyExceptions() {
        doThrow(feignException(404)).when(customPrintRequestClient).getRequestDetails(requestId);
        assertThatThrownBy(() -> customPrintRequestService.getRequestDetailsForAdmin(requestId))
                .isInstanceOf(CustomPrintRequestNotFoundException.class);

        doThrow(feignException(400)).when(customPrintRequestClient).getRequestDetails(requestId);
        assertThatThrownBy(() -> customPrintRequestService.getRequestDetailsForAdmin(requestId))
                .isInstanceOf(CustomPrintRequestOperationFailedException.class);

        doThrow(feignException(503)).when(customPrintRequestClient).getRequestDetails(requestId);
        assertThatThrownBy(() -> customPrintRequestService.getRequestDetailsForAdmin(requestId))
                .isInstanceOf(CustomPrintServiceUnavailableException.class);
    }

    @Test
    void customerAndAdminSearchMapFiltersToFeignParameters() {
        CustomPrintSearchRequest searchRequest = new CustomPrintSearchRequest();
        searchRequest.setKeyword(" bracket ");
        searchRequest.setStatus(CustomPrintRequestStatus.PENDING_REVIEW);
        searchRequest.setCreatedFrom(LocalDate.of(2026, 1, 1));
        searchRequest.setCreatedTo(LocalDate.of(2026, 1, 2));

        customPrintRequestService.getCustomerRequests(customerId, searchRequest);
        customPrintRequestService.getAllRequestsForAdmin(searchRequest);

        verify(customPrintRequestClient).getCustomerRequests(
                eq(customerId),
                eq("bracket"),
                eq(CustomPrintRequestStatus.PENDING_REVIEW),
                eq("2026-01-01T00:00:00"),
                eq("2026-01-02T23:59:59")
        );
        verify(customPrintRequestClient).getAllRequests(
                eq("bracket"),
                eq(CustomPrintRequestStatus.PENDING_REVIEW),
                eq("2026-01-01T00:00:00"),
                eq("2026-01-02T23:59:59")
        );
    }

    @Test
    void invalidSearchDateRangeIsRejectedBeforeFeignCall() {
        CustomPrintSearchRequest searchRequest = new CustomPrintSearchRequest();
        searchRequest.setCreatedFrom(LocalDate.of(2026, 2, 1));
        searchRequest.setCreatedTo(LocalDate.of(2026, 1, 1));

        assertThatThrownBy(() -> customPrintRequestService.getCustomerRequests(customerId, searchRequest))
                .isInstanceOf(CustomPrintRequestOperationFailedException.class);
    }

    @Test
    void hideAndArchiveCallFeignClient() {
        customPrintRequestService.hideCustomerRequest(customerId, requestId);
        customPrintRequestService.archiveRequest(requestId);

        verify(customPrintRequestClient).hideCustomerRequest(customerId, requestId);
        verify(customPrintRequestClient).archiveRequest(requestId);
    }

    private FeignException feignException(int status) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "/api/custom-print-requests/" + requestId,
                Map.of(),
                null,
                StandardCharsets.UTF_8
        );

        Response response = Response.builder()
                .status(status)
                .reason("error")
                .request(request)
                .headers(Map.of())
                .build();

        return FeignException.errorStatus("getRequestDetails", response);
    }
}
