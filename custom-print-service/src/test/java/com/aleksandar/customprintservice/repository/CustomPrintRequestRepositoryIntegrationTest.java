package com.aleksandar.customprintservice.repository;

import com.aleksandar.customprintservice.CustomPrintRequestTestData;
import com.aleksandar.customprintservice.model.entity.CustomPrintRequest;
import com.aleksandar.customprintservice.model.enums.CustomPrintRequestStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CustomPrintRequestRepositoryIntegrationTest {

    @Autowired
    private CustomPrintRequestRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findsAdminAttentionCandidates() {
        UUID stalePending = saveWithUpdatedOn(CustomPrintRequestStatus.PENDING_REVIEW, LocalDateTime.now().minusHours(49));
        UUID staleChanges = saveWithUpdatedOn(CustomPrintRequestStatus.CHANGES_REQUESTED, LocalDateTime.now().minusHours(49));
        saveWithUpdatedOn(CustomPrintRequestStatus.PENDING_REVIEW, LocalDateTime.now().minusHours(1));

        List<CustomPrintRequest> result = repository.findRequestsWaitingForAdminAttention(
                List.of(CustomPrintRequestStatus.PENDING_REVIEW, CustomPrintRequestStatus.CHANGES_REQUESTED),
                LocalDateTime.now().minusHours(48)
        );

        assertThat(result).extracting(CustomPrintRequest::getId).containsExactlyInAnyOrder(stalePending, staleChanges);
    }

    @Test
    void findsCustomerReminderCandidates() {
        UUID staleOffer = saveOfferWithQuotedOn(LocalDateTime.now().minusHours(73), false);
        saveOfferWithQuotedOn(LocalDateTime.now().minusHours(1), false);
        saveOfferWithQuotedOn(LocalDateTime.now().minusHours(80), true);

        List<CustomPrintRequest> result = repository.findOffersWaitingForCustomerResponse(
                CustomPrintRequestStatus.OFFER_SENT,
                LocalDateTime.now().minusHours(72)
        );

        assertThat(result).extracting(CustomPrintRequest::getId).containsExactly(staleOffer);
    }

    @Test
    void findsAutoArchiveCandidates() {
        UUID delivered = saveWithUpdatedOn(CustomPrintRequestStatus.DELIVERED, LocalDateTime.now().minusDays(31));
        UUID cancelled = saveWithUpdatedOn(CustomPrintRequestStatus.CANCELLED, LocalDateTime.now().minusDays(31));
        UUID rejected = saveWithUpdatedOn(CustomPrintRequestStatus.REJECTED, LocalDateTime.now().minusDays(31));
        saveWithUpdatedOn(CustomPrintRequestStatus.DELIVERED, LocalDateTime.now().minusDays(1));

        List<CustomPrintRequest> result = repository.findOldFinishedRequestsVisibleToAdmin(
                List.of(CustomPrintRequestStatus.DELIVERED, CustomPrintRequestStatus.CANCELLED, CustomPrintRequestStatus.REJECTED),
                LocalDateTime.now().minusDays(30)
        );

        assertThat(result).extracting(CustomPrintRequest::getId).containsExactlyInAnyOrder(delivered, cancelled, rejected);
    }

    @Test
    void customerSearchExcludesHiddenFromCustomerAndAppliesFilters() {
        CustomPrintRequest visible = CustomPrintRequestTestData.validEntity(CustomPrintRequestStatus.PENDING_REVIEW);
        visible.setTitle("Visible phone stand");
        UUID visibleId = repository.saveAndFlush(visible).getId();

        CustomPrintRequest hidden = CustomPrintRequestTestData.validEntity(CustomPrintRequestStatus.PENDING_REVIEW);
        hidden.setTitle("Hidden phone stand");
        hidden.setHiddenFromCustomer(true);
        repository.saveAndFlush(hidden);

        List<CustomPrintRequest> result = repository.searchCustomerRequests(
                CustomPrintRequestTestData.CUSTOMER_ID,
                "phone",
                CustomPrintRequestStatus.PENDING_REVIEW,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1)
        );

        assertThat(result).extracting(CustomPrintRequest::getId).containsExactly(visibleId);
    }

    @Test
    void adminSearchExcludesHiddenFromAdminAndSearchesCustomerFields() {
        CustomPrintRequest visible = CustomPrintRequestTestData.validEntity(CustomPrintRequestStatus.REJECTED);
        visible.setCustomerUsername("needle-user");
        UUID visibleId = repository.saveAndFlush(visible).getId();

        CustomPrintRequest hidden = CustomPrintRequestTestData.validEntity(CustomPrintRequestStatus.REJECTED);
        hidden.setCustomerUsername("needle-hidden");
        hidden.setHiddenFromAdmin(true);
        repository.saveAndFlush(hidden);

        List<CustomPrintRequest> result = repository.searchAdminRequests(
                "needle",
                CustomPrintRequestStatus.REJECTED,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1)
        );

        assertThat(result).extracting(CustomPrintRequest::getId).containsExactly(visibleId);
    }

    private UUID saveWithUpdatedOn(CustomPrintRequestStatus status, LocalDateTime updatedOn) {
        UUID id = repository.saveAndFlush(CustomPrintRequestTestData.validEntity(status)).getId();
        jdbcTemplate.update(
                "update custom_print_requests set updated_on = ? where id = ?",
                Timestamp.valueOf(updatedOn),
                id
        );
        entityManager.clear();
        return id;
    }

    private UUID saveOfferWithQuotedOn(LocalDateTime quotedOn, boolean alreadyMarked) {
        CustomPrintRequest request = CustomPrintRequestTestData.validEntity(CustomPrintRequestStatus.OFFER_SENT);
        request.setQuotedOn(quotedOn);
        request.setCustomerResponseReminderRequired(alreadyMarked);
        if (alreadyMarked) {
            request.setCustomerResponseReminderMarkedOn(LocalDateTime.now().minusHours(1));
        }
        return repository.saveAndFlush(request).getId();
    }
}
