package com.aleksandar.threedforgemarket.web.controller.admin;

import com.aleksandar.threedforgemarket.service.report.AdminReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.aleksandar.threedforgemarket.testsecurity.MarketplaceSecurityTestSupport.admin;
import static com.aleksandar.threedforgemarket.testsecurity.MarketplaceSecurityTestSupport.customer;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminReportControllerMvcTest {
    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminReportService adminReportService;

    @Test
    void reportsPageRequiresAdmin() throws Exception {
        mockMvc.perform(get("/admin/reports"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));

        mockMvc.perform(get("/admin/reports").with(customer(UUID.randomUUID())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/reports").with(admin(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reports"))
                .andExpect(content().string(containsString("Admin reports")));
    }

    @Test
    void usersReportReturnsXlsxAttachment() throws Exception {
        when(adminReportService.generateUsersReport()).thenReturn(reportBytes());

        mockMvc.perform(get("/admin/reports/users.xlsx").with(admin(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(XLSX_CONTENT_TYPE))
                .andExpect(header().string("Content-Disposition", containsString("users-report.xlsx")));
    }

    @Test
    void ordersReportReturnsXlsxAttachment() throws Exception {
        when(adminReportService.generateOrdersReport()).thenReturn(reportBytes());

        mockMvc.perform(get("/admin/reports/orders.xlsx").with(admin(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(XLSX_CONTENT_TYPE))
                .andExpect(header().string("Content-Disposition", containsString("orders-report.xlsx")));
    }

    @Test
    void customPrintsReportReturnsXlsxAttachment() throws Exception {
        when(adminReportService.generateCustomPrintsReport()).thenReturn(reportBytes());

        mockMvc.perform(get("/admin/reports/custom-prints.xlsx").with(admin(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(XLSX_CONTENT_TYPE))
                .andExpect(header().string("Content-Disposition", containsString("custom-print-requests-report.xlsx")));
    }

    @Test
    void fullReportReturnsXlsxAttachment() throws Exception {
        when(adminReportService.generateFullReport()).thenReturn(reportBytes());

        mockMvc.perform(get("/admin/reports/full.xlsx").with(admin(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(XLSX_CONTENT_TYPE))
                .andExpect(header().string("Content-Disposition", containsString("admin-report.xlsx")));
    }

    private byte[] reportBytes() {
        return new byte[]{1, 2, 3};
    }
}
