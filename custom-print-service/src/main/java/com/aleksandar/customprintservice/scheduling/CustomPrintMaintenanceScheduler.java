package com.aleksandar.customprintservice.scheduling;

import com.aleksandar.customprintservice.service.CustomPrintMaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "custom-print.maintenance.scheduling-enabled",
        havingValue = "true",
        matchIfMissing = true
)
@RequiredArgsConstructor
public class CustomPrintMaintenanceScheduler {

    private final CustomPrintMaintenanceService customPrintMaintenanceService;

    @Scheduled(cron = "${custom-print.maintenance.admin-attention-cron}")
    public void runNightlyMaintenance() {
        customPrintMaintenanceService.markRequestsWaitingForAdminAttention();
        customPrintMaintenanceService.archiveOldFinishedRequests();
    }

    @Scheduled(fixedDelayString = "${custom-print.maintenance.customer-reminder-delay}")
    public void runCustomerReminderMaintenance() {
        customPrintMaintenanceService.markOffersWaitingForCustomerResponse();
    }
}
