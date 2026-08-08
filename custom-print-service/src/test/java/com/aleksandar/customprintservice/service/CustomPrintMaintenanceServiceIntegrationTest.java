package com.aleksandar.customprintservice.service;

import com.aleksandar.customprintservice.CustomPrintRequestTestData;
import com.aleksandar.customprintservice.model.entity.CustomPrintRequest;
import com.aleksandar.customprintservice.model.enums.CustomPrintRequestStatus;
import com.aleksandar.customprintservice.repository.CustomPrintRequestRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CustomPrintMaintenanceServiceIntegrationTest {

    @Autowired
    private CustomPrintMaintenanceService maintenanceService;

    @Autowired
    private CustomPrintRequestRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void stalePendingReviewRequestGetsAdminAttention() {
        UUID requestId = saveWithUpdatedOn(CustomPrintRequestStatus.PENDING_REVIEW, LocalDateTime.now().minusHours(49));

        maintenanceService.markRequestsWaitingForAdminAttention();

        CustomPrintRequest request = findFresh(requestId);
        assertThat(request.isAdminAttentionRequired()).isTrue();
        assertThat(request.getAdminAttentionMarkedOn()).isNotNull();
    }

    @Test
    void staleChangesRequestedRequestGetsAdminAttention() {
        UUID requestId = saveWithUpdatedOn(CustomPrintRequestStatus.CHANGES_REQUESTED, LocalDateTime.now().minusHours(49));

        maintenanceService.markRequestsWaitingForAdminAttention();

        CustomPrintRequest request = findFresh(requestId);
        assertThat(request.isAdminAttentionRequired()).isTrue();
        assertThat(request.getAdminAttentionMarkedOn()).isNotNull();
    }

    @Test
    void freshPendingRequestIsNotMarkedForAdminAttention() {
        UUID requestId = saveWithUpdatedOn(CustomPrintRequestStatus.PENDING_REVIEW, LocalDateTime.now().minusHours(2));

        maintenanceService.markRequestsWaitingForAdminAttention();

        CustomPrintRequest request = findFresh(requestId);
        assertThat(request.isAdminAttentionRequired()).isFalse();
        assertThat(request.getAdminAttentionMarkedOn()).isNull();
    }

    @Test
    void staleOfferSentRequestGetsCustomerResponseReminder() {
        UUID requestId = saveOfferWithQuotedOn(LocalDateTime.now().minusHours(73));

        maintenanceService.markOffersWaitingForCustomerResponse();

        CustomPrintRequest request = findFresh(requestId);
        assertThat(request.isCustomerResponseReminderRequired()).isTrue();
        assertThat(request.getCustomerResponseReminderMarkedOn()).isNotNull();
    }

    @Test
    void freshOfferIsNotMarkedForCustomerResponseReminder() {
        UUID requestId = saveOfferWithQuotedOn(LocalDateTime.now().minusHours(5));

        maintenanceService.markOffersWaitingForCustomerResponse();

        CustomPrintRequest request = findFresh(requestId);
        assertThat(request.isCustomerResponseReminderRequired()).isFalse();
        assertThat(request.getCustomerResponseReminderMarkedOn()).isNull();
    }

    @Test
    void oldFinishedRequestsAreHiddenFromAdminAndAutoArchived() {
        UUID deliveredId = saveWithUpdatedOn(CustomPrintRequestStatus.DELIVERED, LocalDateTime.now().minusDays(31));
        UUID cancelledId = saveWithUpdatedOn(CustomPrintRequestStatus.CANCELLED, LocalDateTime.now().minusDays(31));
        UUID rejectedId = saveWithUpdatedOn(CustomPrintRequestStatus.REJECTED, LocalDateTime.now().minusDays(31));

        maintenanceService.archiveOldFinishedRequests();

        assertAutoArchived(deliveredId);
        assertAutoArchived(cancelledId);
        assertAutoArchived(rejectedId);
    }

    @Test
    void autoArchiveDoesNotHideRequestFromCustomer() {
        UUID requestId = saveWithUpdatedOn(CustomPrintRequestStatus.DELIVERED, LocalDateTime.now().minusDays(31));

        maintenanceService.archiveOldFinishedRequests();

        CustomPrintRequest request = findFresh(requestId);
        assertThat(request.isHiddenFromAdmin()).isTrue();
        assertThat(request.isHiddenFromCustomer()).isFalse();
    }

    @Test
    void alreadyMarkedOrHiddenRecordsAreNotUpdatedAgain() {
        LocalDateTime originalMarkedOn = LocalDateTime.now().minusDays(3).withNano(0);
        CustomPrintRequest marked = CustomPrintRequestTestData.validEntity(CustomPrintRequestStatus.PENDING_REVIEW);
        marked.setAdminAttentionRequired(true);
        marked.setAdminAttentionMarkedOn(originalMarkedOn);
        UUID markedId = repository.saveAndFlush(marked).getId();
        setUpdatedOn(markedId, LocalDateTime.now().minusHours(60));

        CustomPrintRequest hidden = CustomPrintRequestTestData.validEntity(CustomPrintRequestStatus.DELIVERED);
        hidden.setHiddenFromAdmin(true);
        hidden.setHiddenFromAdminOn(originalMarkedOn);
        UUID hiddenId = repository.saveAndFlush(hidden).getId();
        setUpdatedOn(hiddenId, LocalDateTime.now().minusDays(40));

        maintenanceService.markRequestsWaitingForAdminAttention();
        maintenanceService.archiveOldFinishedRequests();

        assertThat(findFresh(markedId).getAdminAttentionMarkedOn()).isEqualTo(originalMarkedOn);
        assertThat(findFresh(hiddenId).getAutoArchivedOn()).isNull();
    }

    private UUID saveWithUpdatedOn(CustomPrintRequestStatus status, LocalDateTime updatedOn) {
        UUID id = repository.saveAndFlush(CustomPrintRequestTestData.validEntity(status)).getId();
        setUpdatedOn(id, updatedOn);
        return id;
    }

    private UUID saveOfferWithQuotedOn(LocalDateTime quotedOn) {
        CustomPrintRequest request = CustomPrintRequestTestData.validEntity(CustomPrintRequestStatus.OFFER_SENT);
        request.setQuotedOn(quotedOn);
        return repository.saveAndFlush(request).getId();
    }

    private void setUpdatedOn(UUID id, LocalDateTime updatedOn) {
        jdbcTemplate.update(
                "update custom_print_requests set updated_on = ? where id = ?",
                Timestamp.valueOf(updatedOn),
                id
        );
        entityManager.clear();
    }

    private CustomPrintRequest findFresh(UUID id) {
        entityManager.clear();
        return repository.findById(id).orElseThrow();
    }

    private void assertAutoArchived(UUID id) {
        CustomPrintRequest request = findFresh(id);
        assertThat(request.isHiddenFromAdmin()).isTrue();
        assertThat(request.getHiddenFromAdminOn()).isNotNull();
        assertThat(request.getAutoArchivedOn()).isNotNull();
    }
}
