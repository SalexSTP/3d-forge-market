package com.aleksandar.threedforgemarket.web.controller.admin;

import com.aleksandar.threedforgemarket.exception.customprint.CustomPrintServiceUnavailableException;
import com.aleksandar.threedforgemarket.service.report.AdminReportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/admin/reports")
public class AdminReportController {
    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final AdminReportService adminReportService;

    public AdminReportController(AdminReportService adminReportService) {
        this.adminReportService = adminReportService;
    }

    @GetMapping
    public ModelAndView getReportsPage() {
        return new ModelAndView("admin/reports");
    }

    @GetMapping("/users.xlsx")
    public ResponseEntity<byte[]> exportUsersReport() {
        return xlsx(adminReportService.generateUsersReport(), "users-report.xlsx");
    }

    @GetMapping("/orders.xlsx")
    public ResponseEntity<byte[]> exportOrdersReport() {
        return xlsx(adminReportService.generateOrdersReport(), "orders-report.xlsx");
    }

    @GetMapping("/custom-prints.xlsx")
    public ResponseEntity<byte[]> exportCustomPrintsReport() {
        return xlsx(adminReportService.generateCustomPrintsReport(), "custom-print-requests-report.xlsx");
    }

    @GetMapping("/full.xlsx")
    public ResponseEntity<byte[]> exportFullReport() {
        return xlsx(adminReportService.generateFullReport(), "admin-report.xlsx");
    }

    @ExceptionHandler(CustomPrintServiceUnavailableException.class)
    public ModelAndView handleCustomPrintUnavailable(CustomPrintServiceUnavailableException exception) {
        ModelAndView modelAndView = new ModelAndView("admin/reports");
        modelAndView.addObject("errorMessage", "Custom print service is currently unavailable.");
        return modelAndView;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ModelAndView handleReportFailure(IllegalStateException exception) {
        ModelAndView modelAndView = new ModelAndView("admin/reports");
        modelAndView.addObject("errorMessage", "Unable to generate report right now.");
        return modelAndView;
    }

    private ResponseEntity<byte[]> xlsx(byte[] content, String filename) {
        return ResponseEntity.ok()
                .contentType(XLSX_MEDIA_TYPE)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString()
                )
                .body(content);
    }
}
