package com.aleksandar.customprintservice.scheduling;

import com.aleksandar.customprintservice.service.CustomPrintMaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
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
