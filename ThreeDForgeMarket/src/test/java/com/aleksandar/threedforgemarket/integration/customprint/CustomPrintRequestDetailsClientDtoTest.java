package com.aleksandar.threedforgemarket.integration.customprint;

import com.aleksandar.threedforgemarket.testdata.CustomPrintClientTestData;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomPrintRequestDetailsClientDtoTest {

    @Test
    void statusHelpersReflectPendingReviewState() {
        CustomPrintRequestDetailsClientDto details = CustomPrintClientTestData.details(
                UUID.randomUUID(),
                UUID.randomUUID(),
                CustomPrintRequestStatus.PENDING_REVIEW
        );

        assertThat(details.isPendingReview()).isTrue();
        assertThat(details.isEditable()).isTrue();
        assertThat(details.isAdminActionable()).isTrue();
        assertThat(details.isCancellable()).isTrue();
        assertThat(details.isOfferSent()).isFalse();
    }

    @Test
    void statusHelpersReflectOfferAndFulfillmentStates() {
        CustomPrintRequestDetailsClientDto offer = CustomPrintClientTestData.details(
                UUID.randomUUID(),
                UUID.randomUUID(),
                CustomPrintRequestStatus.OFFER_SENT
        );
        CustomPrintRequestDetailsClientDto accepted = CustomPrintClientTestData.details(
                UUID.randomUUID(),
                UUID.randomUUID(),
                CustomPrintRequestStatus.ACCEPTED
        );
        CustomPrintRequestDetailsClientDto printing = CustomPrintClientTestData.details(
                UUID.randomUUID(),
                UUID.randomUUID(),
                CustomPrintRequestStatus.PRINTING
        );
        CustomPrintRequestDetailsClientDto ready = CustomPrintClientTestData.details(
                UUID.randomUUID(),
                UUID.randomUUID(),
                CustomPrintRequestStatus.READY_FOR_DELIVERY
        );

        assertThat(offer.isAcceptable()).isTrue();
        assertThat(offer.isChangeRequestAllowed()).isTrue();
        assertThat(accepted.getNextFulfillmentStatus()).isEqualTo(CustomPrintRequestStatus.PRINTING);
        assertThat(accepted.getNextFulfillmentActionLabel()).isEqualTo("Start printing");
        assertThat(printing.getNextFulfillmentStatus()).isEqualTo(CustomPrintRequestStatus.READY_FOR_DELIVERY);
        assertThat(printing.getNextFulfillmentActionLabel()).isEqualTo("Mark ready for delivery");
        assertThat(ready.getNextFulfillmentStatus()).isEqualTo(CustomPrintRequestStatus.DELIVERED);
        assertThat(ready.getNextFulfillmentActionLabel()).isEqualTo("Mark delivered");
    }

    @Test
    void removableAndTerminalStatusHelpersReflectFinishedStates() {
        CustomPrintRequestDetailsClientDto delivered = CustomPrintClientTestData.details(
                UUID.randomUUID(),
                UUID.randomUUID(),
                CustomPrintRequestStatus.DELIVERED
        );
        CustomPrintRequestDetailsClientDto rejected = CustomPrintClientTestData.details(
                UUID.randomUUID(),
                UUID.randomUUID(),
                CustomPrintRequestStatus.REJECTED
        );

        assertThat(delivered.isDelivered()).isTrue();
        assertThat(delivered.isCustomerRemovable()).isTrue();
        assertThat(delivered.isAdminArchivable()).isTrue();
        assertThat(delivered.getAvailableFulfillmentStatuses()).isEmpty();
        assertThat(delivered.getNextFulfillmentActionLabel()).isEqualTo("Update status");
        assertThat(rejected.isAdminArchivable()).isTrue();
    }
}
