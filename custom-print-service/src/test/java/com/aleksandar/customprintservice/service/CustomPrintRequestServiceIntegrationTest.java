package com.aleksandar.customprintservice.service;

import com.aleksandar.customprintservice.CustomPrintRequestTestData;
import com.aleksandar.customprintservice.exception.CustomPrintRequestNotFoundException;
import com.aleksandar.customprintservice.exception.CustomPrintRequestOperationNotAllowedException;
import com.aleksandar.customprintservice.model.dto.CustomPrintRequestDetailsDto;
import com.aleksandar.customprintservice.model.entity.CustomPrintRequest;
import com.aleksandar.customprintservice.model.enums.CustomPrintRequestStatus;
import com.aleksandar.customprintservice.repository.CustomPrintRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CustomPrintRequestServiceIntegrationTest {

    @Autowired
    private CustomPrintRequestService service;

    @Autowired
    private CustomPrintRequestRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void createRequestStartsAsPendingReview() {
        CustomPrintRequestDetailsDto result = service.createRequest(CustomPrintRequestTestData.validCreateDto());

        assertThat(result.id()).isNotNull();
        assertThat(result.status()).isEqualTo(CustomPrintRequestStatus.PENDING_REVIEW);
        assertThat(result.adminAttentionRequired()).isFalse();
        assertThat(result.customerResponseReminderRequired()).isFalse();
    }

    @Test
    void updateCustomerRequestSucceedsOnlyWhilePendingReview() {
        UUID requestId = service.createRequest(CustomPrintRequestTestData.validCreateDto()).id();

        CustomPrintRequestDetailsDto result = service.updateCustomerRequest(
                requestId,
                CustomPrintRequestTestData.CUSTOMER_ID,
                CustomPrintRequestTestData.validUpdateDto()
        );

        assertThat(result.title()).isEqualTo("Updated phone stand");
        assertThat(result.material()).isEqualTo("PETG");
    }

    @Test
    void updateCustomerRequestFailsAfterOfferSent() {
        UUID requestId = createOfferSentRequest();

        assertThatThrownBy(() -> service.updateCustomerRequest(
                requestId,
                CustomPrintRequestTestData.CUSTOMER_ID,
                CustomPrintRequestTestData.validUpdateDto()
        ))
                .isInstanceOf(CustomPrintRequestOperationNotAllowedException.class)
                .hasMessage("Only pending custom print requests can be edited.");
    }

    @Test
    void sendOfferFromPendingReviewClearsAdminAttention() {
        UUID requestId = repository.save(requestWithAdminAttention(CustomPrintRequestStatus.PENDING_REVIEW)).getId();

        CustomPrintRequestDetailsDto result = service.sendOffer(requestId, CustomPrintRequestTestData.validOfferDto());

        assertThat(result.status()).isEqualTo(CustomPrintRequestStatus.OFFER_SENT);
        assertThat(result.quotedPrice()).isEqualByComparingTo("49.99");
        assertThat(result.adminAttentionRequired()).isFalse();
        assertThat(result.adminAttentionMarkedOn()).isNull();
    }

    @Test
    void sendOfferFromChangesRequestedSucceeds() {
        UUID requestId = createOfferSentRequest();
        service.requestChanges(requestId, CustomPrintRequestTestData.CUSTOMER_ID, CustomPrintRequestTestData.validChangesDto());

        CustomPrintRequestDetailsDto result = service.sendOffer(requestId, CustomPrintRequestTestData.validOfferDto());

        assertThat(result.status()).isEqualTo(CustomPrintRequestStatus.OFFER_SENT);
        assertThat(result.quotedOn()).isNotNull();
    }

    @Test
    void rejectRequestFromPendingReviewClearsAdminAttention() {
        UUID requestId = repository.save(requestWithAdminAttention(CustomPrintRequestStatus.PENDING_REVIEW)).getId();

        CustomPrintRequestDetailsDto result = service.rejectRequest(requestId, CustomPrintRequestTestData.validRejectDto());

        assertThat(result.status()).isEqualTo(CustomPrintRequestStatus.REJECTED);
        assertThat(result.adminMessage()).contains("not printable");
        assertThat(result.adminAttentionRequired()).isFalse();
        assertThat(result.adminAttentionMarkedOn()).isNull();
    }

    @Test
    void acceptOfferFromOfferSentClearsCustomerReminder() {
        UUID requestId = repository.save(requestWithCustomerReminder()).getId();

        CustomPrintRequestDetailsDto result = service.acceptOffer(requestId, CustomPrintRequestTestData.CUSTOMER_ID);

        assertThat(result.status()).isEqualTo(CustomPrintRequestStatus.ACCEPTED);
        assertThat(result.acceptedOn()).isNotNull();
        assertThat(result.customerResponseReminderRequired()).isFalse();
        assertThat(result.customerResponseReminderMarkedOn()).isNull();
    }

    @Test
    void acceptOfferFailsWhenNotOfferSent() {
        UUID requestId = service.createRequest(CustomPrintRequestTestData.validCreateDto()).id();

        assertThatThrownBy(() -> service.acceptOffer(requestId, CustomPrintRequestTestData.CUSTOMER_ID))
                .isInstanceOf(CustomPrintRequestOperationNotAllowedException.class)
                .hasMessage("Only offer-sent custom print requests can be accepted.");
    }

    @Test
    void requestChangesFromOfferSentClearsCustomerReminder() {
        UUID requestId = repository.save(requestWithCustomerReminder()).getId();

        CustomPrintRequestDetailsDto result = service.requestChanges(
                requestId,
                CustomPrintRequestTestData.CUSTOMER_ID,
                CustomPrintRequestTestData.validChangesDto()
        );

        assertThat(result.status()).isEqualTo(CustomPrintRequestStatus.CHANGES_REQUESTED);
        assertThat(result.customerMessage()).contains("reduce");
        assertThat(result.customerResponseReminderRequired()).isFalse();
        assertThat(result.customerResponseReminderMarkedOn()).isNull();
    }

    @Test
    void requestChangesFailsWhenNotOfferSent() {
        UUID requestId = service.createRequest(CustomPrintRequestTestData.validCreateDto()).id();

        assertThatThrownBy(() -> service.requestChanges(
                requestId,
                CustomPrintRequestTestData.CUSTOMER_ID,
                CustomPrintRequestTestData.validChangesDto()
        ))
                .isInstanceOf(CustomPrintRequestOperationNotAllowedException.class)
                .hasMessage("Only offer-sent custom print requests can receive customer change requests.");
    }

    @ParameterizedTest
    @EnumSource(value = CustomPrintRequestStatus.class, names = {"PENDING_REVIEW", "OFFER_SENT", "CHANGES_REQUESTED"})
    void cancelCustomerRequestSucceedsFromAllowedStates(CustomPrintRequestStatus status) {
        UUID requestId = repository.save(requestWithBothMaintenanceFlags(status)).getId();

        CustomPrintRequestDetailsDto result = service.cancelCustomerRequest(requestId, CustomPrintRequestTestData.CUSTOMER_ID);

        assertThat(result.status()).isEqualTo(CustomPrintRequestStatus.CANCELLED);
        assertThat(result.cancelledOn()).isNotNull();
        assertThat(result.adminAttentionRequired()).isFalse();
        assertThat(result.customerResponseReminderRequired()).isFalse();
    }

    @Test
    void cancelCustomerRequestFailsAfterAccepted() {
        UUID requestId = createAcceptedRequest();

        assertThatThrownBy(() -> service.cancelCustomerRequest(requestId, CustomPrintRequestTestData.CUSTOMER_ID))
                .isInstanceOf(CustomPrintRequestOperationNotAllowedException.class)
                .hasMessage("Only pending, offer-sent, or change-requested custom print requests can be cancelled.");
    }

    @Test
    void fulfillmentProgressesThroughProductionStatuses() {
        UUID requestId = createAcceptedRequest();

        CustomPrintRequestDetailsDto printing = service.updateFulfillmentStatus(
                requestId,
                CustomPrintRequestTestData.fulfillmentDto(CustomPrintRequestStatus.PRINTING)
        );
        CustomPrintRequestDetailsDto ready = service.updateFulfillmentStatus(
                requestId,
                CustomPrintRequestTestData.fulfillmentDto(CustomPrintRequestStatus.READY_FOR_DELIVERY)
        );
        CustomPrintRequestDetailsDto delivered = service.updateFulfillmentStatus(
                requestId,
                CustomPrintRequestTestData.fulfillmentDto(CustomPrintRequestStatus.DELIVERED)
        );

        assertThat(printing.printingStartedOn()).isNotNull();
        assertThat(ready.readyForDeliveryOn()).isNotNull();
        assertThat(delivered.status()).isEqualTo(CustomPrintRequestStatus.DELIVERED);
        assertThat(delivered.deliveredOn()).isNotNull();
    }

    @Test
    void skippingFulfillmentStatusesFails() {
        UUID requestId = createAcceptedRequest();

        assertThatThrownBy(() -> service.updateFulfillmentStatus(
                requestId,
                CustomPrintRequestTestData.fulfillmentDto(CustomPrintRequestStatus.READY_FOR_DELIVERY)
        ))
                .isInstanceOf(CustomPrintRequestOperationNotAllowedException.class)
                .hasMessage("Custom print fulfillment status can only move to the next production step.");
    }

    @Test
    void customerCanHideFinalRequest() {
        UUID requestId = createCancelledRequest();

        CustomPrintRequestDetailsDto result = service.hideCustomerRequest(requestId, CustomPrintRequestTestData.CUSTOMER_ID);

        assertThat(result.id()).isEqualTo(requestId);
        assertThat(repository.findById(requestId)).get().extracting(CustomPrintRequest::isHiddenFromCustomer).isEqualTo(true);
        assertThat(repository.findById(requestId)).get().extracting(CustomPrintRequest::getHiddenFromCustomerOn).isNotNull();
    }

    @Test
    void customerCannotHideNonFinalRequest() {
        UUID requestId = service.createRequest(CustomPrintRequestTestData.validCreateDto()).id();

        assertThatThrownBy(() -> service.hideCustomerRequest(requestId, CustomPrintRequestTestData.CUSTOMER_ID))
                .isInstanceOf(CustomPrintRequestOperationNotAllowedException.class)
                .hasMessage("Only cancelled, rejected, or delivered custom print requests can be removed from lists.");
    }

    @Test
    void adminCanArchiveFinalRequest() {
        UUID requestId = createCancelledRequest();

        service.archiveRequest(requestId);

        assertThat(repository.findById(requestId)).get().extracting(CustomPrintRequest::isHiddenFromAdmin).isEqualTo(true);
        assertThat(repository.findById(requestId)).get().extracting(CustomPrintRequest::getHiddenFromAdminOn).isNotNull();
    }

    @Test
    void adminCannotArchiveNonFinalRequest() {
        UUID requestId = service.createRequest(CustomPrintRequestTestData.validCreateDto()).id();

        assertThatThrownBy(() -> service.archiveRequest(requestId))
                .isInstanceOf(CustomPrintRequestOperationNotAllowedException.class)
                .hasMessage("Only cancelled, rejected, or delivered custom print requests can be removed from lists.");
    }

    @Test
    void accessingAnotherCustomersRequestFailsAsNotFound() {
        UUID requestId = service.createRequest(CustomPrintRequestTestData.validCreateDto()).id();

        assertThatThrownBy(() -> service.getCustomerRequestDetails(requestId, CustomPrintRequestTestData.OTHER_CUSTOMER_ID))
                .isInstanceOf(CustomPrintRequestNotFoundException.class)
                .hasMessage("Custom print request was not found.");
    }

    private UUID createOfferSentRequest() {
        UUID requestId = service.createRequest(CustomPrintRequestTestData.validCreateDto()).id();
        return service.sendOffer(requestId, CustomPrintRequestTestData.validOfferDto()).id();
    }

    private UUID createAcceptedRequest() {
        UUID requestId = createOfferSentRequest();
        return service.acceptOffer(requestId, CustomPrintRequestTestData.CUSTOMER_ID).id();
    }

    private UUID createCancelledRequest() {
        UUID requestId = service.createRequest(CustomPrintRequestTestData.validCreateDto()).id();
        return service.cancelCustomerRequest(requestId, CustomPrintRequestTestData.CUSTOMER_ID).id();
    }

    private CustomPrintRequest requestWithAdminAttention(CustomPrintRequestStatus status) {
        CustomPrintRequest request = CustomPrintRequestTestData.validEntity(status);
        request.setAdminAttentionRequired(true);
        request.setAdminAttentionMarkedOn(java.time.LocalDateTime.now().minusHours(1));
        return request;
    }

    private CustomPrintRequest requestWithCustomerReminder() {
        CustomPrintRequest request = CustomPrintRequestTestData.validEntity(CustomPrintRequestStatus.OFFER_SENT);
        request.setQuotedOn(java.time.LocalDateTime.now().minusHours(80));
        request.setCustomerResponseReminderRequired(true);
        request.setCustomerResponseReminderMarkedOn(java.time.LocalDateTime.now().minusHours(1));
        return request;
    }

    private CustomPrintRequest requestWithBothMaintenanceFlags(CustomPrintRequestStatus status) {
        CustomPrintRequest request = CustomPrintRequestTestData.validEntity(status);
        request.setAdminAttentionRequired(true);
        request.setAdminAttentionMarkedOn(java.time.LocalDateTime.now().minusHours(1));
        request.setCustomerResponseReminderRequired(true);
        request.setCustomerResponseReminderMarkedOn(java.time.LocalDateTime.now().minusHours(1));
        if (status == CustomPrintRequestStatus.OFFER_SENT) {
            request.setQuotedOn(java.time.LocalDateTime.now().minusHours(80));
        }
        return request;
    }
}
