package com.aleksandar.customprintservice.service;

import com.aleksandar.customprintservice.model.entity.CustomPrintRequest;
import com.aleksandar.customprintservice.model.enums.CustomPrintRequestStatus;
import com.aleksandar.customprintservice.repository.CustomPrintRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomPrintMaintenanceService {

    private static final List<CustomPrintRequestStatus> ADMIN_ATTENTION_STATUSES = List.of(
            CustomPrintRequestStatus.PENDING_REVIEW,
            CustomPrintRequestStatus.CHANGES_REQUESTED
    );

    private static final List<CustomPrintRequestStatus> FINISHED_STATUSES = List.of(
            CustomPrintRequestStatus.DELIVERED,
            CustomPrintRequestStatus.CANCELLED,
            CustomPrintRequestStatus.REJECTED
    );

    private final CustomPrintRequestRepository customPrintRequestRepository;

    @Value("${custom-print.maintenance.admin-attention-hours:48}")
    private long adminAttentionHours;

    @Value("${custom-print.maintenance.customer-response-hours:72}")
    private long customerResponseHours;

    @Value("${custom-print.maintenance.auto-archive-days:30}")
    private long autoArchiveDays;

    @Transactional
    public void markRequestsWaitingForAdminAttention() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime staleBefore = now.minusHours(adminAttentionHours);
        List<CustomPrintRequest> requests = customPrintRequestRepository.findRequestsWaitingForAdminAttention(
                ADMIN_ATTENTION_STATUSES,
                staleBefore
        );

        requests.forEach(request -> {
            request.setAdminAttentionRequired(true);
            request.setAdminAttentionMarkedOn(now);
        });

        log.info("Marked {} custom print requests as requiring admin attention", requests.size());
    }

    @Transactional
    public void markOffersWaitingForCustomerResponse() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime staleBefore = now.minusHours(customerResponseHours);
        List<CustomPrintRequest> requests = customPrintRequestRepository.findOffersWaitingForCustomerResponse(
                CustomPrintRequestStatus.OFFER_SENT,
                staleBefore
        );

        requests.forEach(request -> {
            request.setCustomerResponseReminderRequired(true);
            request.setCustomerResponseReminderMarkedOn(now);
        });

        log.info("Marked {} custom print offers as requiring customer response reminder", requests.size());
    }

    @Transactional
    public void archiveOldFinishedRequests() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime staleBefore = now.minusDays(autoArchiveDays);
        List<CustomPrintRequest> requests = customPrintRequestRepository.findOldFinishedRequestsVisibleToAdmin(
                FINISHED_STATUSES,
                staleBefore
        );

        requests.forEach(request -> {
            request.setHiddenFromAdmin(true);
            request.setHiddenFromAdminOn(now);
            request.setAutoArchivedOn(now);
        });

        log.info("Auto-archived {} finished custom print requests from admin view", requests.size());
    }
}
