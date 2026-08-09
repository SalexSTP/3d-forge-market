package com.aleksandar.threedforgemarket.service.report;

import com.aleksandar.threedforgemarket.exception.customprint.CustomPrintServiceUnavailableException;
import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestListItemClientDto;
import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestStatus;
import com.aleksandar.threedforgemarket.model.dto.customprint.CustomPrintSearchRequest;
import com.aleksandar.threedforgemarket.model.dto.payment.PaymentSummaryDto;
import com.aleksandar.threedforgemarket.model.entity.CustomerOrder;
import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.enums.payment.PaymentTargetType;
import com.aleksandar.threedforgemarket.repository.order.CustomerOrderRepository;
import com.aleksandar.threedforgemarket.repository.review.ReviewRepository;
import com.aleksandar.threedforgemarket.repository.user.UserRepository;
import com.aleksandar.threedforgemarket.service.customprint.CustomPrintRequestService;
import com.aleksandar.threedforgemarket.service.payment.PaymentService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AdminReportService {
    private static final List<String> USER_HEADERS = List.of(
            "User ID",
            "Username",
            "Email",
            "Role",
            "Active",
            "Created on",
            "Last login",
            "Deactivated on",
            "Orders count",
            "Reviews count"
    );
    private static final List<String> ORDER_HEADERS = List.of(
            "Order ID",
            "Customer username",
            "Customer email",
            "Product name",
            "Quantity",
            "Total EUR",
            "Payment method",
            "Payment status",
            "Order status",
            "Created on",
            "Updated on",
            "Delivery address",
            "Customer note"
    );
    private static final List<String> CUSTOM_PRINT_HEADERS = List.of(
            "Request ID",
            "Customer username",
            "Customer email",
            "Title",
            "Material",
            "Quantity",
            "Status",
            "Quoted price EUR",
            "Payment method",
            "Payment status",
            "Created on",
            "Updated on",
            "Accepted on",
            "Delivered on",
            "Admin attention required",
            "Customer response reminder required"
    );
    private static final String CUSTOM_PRINT_UNAVAILABLE_MESSAGE = "Custom print service is currently unavailable.";

    private final UserRepository userRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final ReviewRepository reviewRepository;
    private final CustomPrintRequestService customPrintRequestService;
    private final PaymentService paymentService;
    private final ExcelReportWriter excelReportWriter;

    public AdminReportService(
            UserRepository userRepository,
            CustomerOrderRepository customerOrderRepository,
            ReviewRepository reviewRepository,
            CustomPrintRequestService customPrintRequestService,
            PaymentService paymentService,
            ExcelReportWriter excelReportWriter
    ) {
        this.userRepository = userRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.reviewRepository = reviewRepository;
        this.customPrintRequestService = customPrintRequestService;
        this.paymentService = paymentService;
        this.excelReportWriter = excelReportWriter;
    }

    @Transactional(readOnly = true)
    public byte[] generateUsersReport() {
        XSSFWorkbook workbook = excelReportWriter.createWorkbook();
        addUsersSheet(workbook);
        return excelReportWriter.toByteArray(workbook);
    }

    @Transactional(readOnly = true)
    public byte[] generateOrdersReport() {
        XSSFWorkbook workbook = excelReportWriter.createWorkbook();
        addOrdersSheet(workbook);
        return excelReportWriter.toByteArray(workbook);
    }

    @Transactional(readOnly = true)
    public byte[] generateCustomPrintsReport() {
        XSSFWorkbook workbook = excelReportWriter.createWorkbook();
        addCustomPrintsSheet(workbook, customPrintRequests());
        return excelReportWriter.toByteArray(workbook);
    }

    @Transactional(readOnly = true)
    public byte[] generateFullReport() {
        XSSFWorkbook workbook = excelReportWriter.createWorkbook();
        addUsersSheet(workbook);
        addOrdersSheet(workbook);

        try {
            addCustomPrintsSheet(workbook, customPrintRequests());
        } catch (CustomPrintServiceUnavailableException exception) {
            excelReportWriter.addMessageSheet(workbook, "Custom prints", CUSTOM_PRINT_UNAVAILABLE_MESSAGE);
        }

        return excelReportWriter.toByteArray(workbook);
    }

    private void addUsersSheet(XSSFWorkbook workbook) {
        List<List<?>> rows = userRepository.findAll()
                .stream()
                .map(this::userRow)
                .toList();
        excelReportWriter.addSheet(workbook, "Users", USER_HEADERS, rows);
    }

    private List<?> userRow(User user) {
        return List.of(
                value(user.getId()),
                value(user.getUsername()),
                value(user.getEmail()),
                value(user.getRole()),
                user.isActive(),
                value(user.getCreatedOn()),
                value(user.getLastLoginOn()),
                value(user.getDeactivatedOn()),
                customerOrderRepository.countByCustomer_Id(user.getId()),
                reviewRepository.countByAuthor_Id(user.getId())
        );
    }

    private void addOrdersSheet(XSSFWorkbook workbook) {
        List<List<?>> rows = customerOrderRepository.findAllForAdminOrderedByStatus()
                .stream()
                .map(this::orderRow)
                .toList();
        excelReportWriter.addSheet(workbook, "Orders", ORDER_HEADERS, rows);
    }

    private List<?> orderRow(CustomerOrder order) {
        PaymentSummaryDto paymentSummary = paymentService
                .getLatestPaymentSummary(PaymentTargetType.PRODUCT_ORDER, order.getId())
                .orElse(null);

        return List.of(
                value(order.getId()),
                value(order.getCustomer().getUsername()),
                value(order.getCustomer().getEmail()),
                value(order.getProduct().getName()),
                value(order.getQuantity()),
                value(order.getTotalPrice()),
                paymentMethod(paymentSummary),
                paymentStatus(paymentSummary),
                value(order.getStatus().getDisplayName()),
                value(order.getCreatedOn()),
                value(order.getUpdatedOn()),
                value(order.getDeliveryAddress()),
                value(order.getCustomerNote())
        );
    }

    private void addCustomPrintsSheet(
            XSSFWorkbook workbook,
            List<CustomPrintRequestListItemClientDto> customPrintRequests
    ) {
        List<List<?>> rows = customPrintRequests.stream()
                .map(this::customPrintRow)
                .toList();
        excelReportWriter.addSheet(workbook, "Custom prints", CUSTOM_PRINT_HEADERS, rows);
    }

    private List<?> customPrintRow(CustomPrintRequestListItemClientDto request) {
        PaymentSummaryDto paymentSummary = paymentService
                .getLatestPaymentSummary(
                        PaymentTargetType.CUSTOM_PRINT_REQUEST,
                        request.id(),
                        isCancelledBeforePayment(request.status())
                )
                .orElse(null);

        return List.of(
                value(request.id()),
                value(request.customerUsername()),
                value(request.customerEmail()),
                value(request.title()),
                value(request.material()),
                value(request.quantity()),
                value(request.status().getDisplayName()),
                value(request.quotedPrice()),
                paymentMethod(paymentSummary),
                paymentStatus(paymentSummary),
                value(request.createdOn()),
                value(request.updatedOn()),
                value(request.acceptedOn()),
                value(request.deliveredOn()),
                Boolean.TRUE.equals(request.adminAttentionRequired()),
                Boolean.TRUE.equals(request.customerResponseReminderRequired())
        );
    }

    private List<CustomPrintRequestListItemClientDto> customPrintRequests() {
        return customPrintRequestService.getAllRequestsForAdmin(new CustomPrintSearchRequest());
    }

    private boolean isCancelledBeforePayment(CustomPrintRequestStatus status) {
        return status == CustomPrintRequestStatus.CANCELLED
                || status == CustomPrintRequestStatus.REJECTED;
    }

    private String paymentMethod(PaymentSummaryDto paymentSummary) {
        return paymentSummary == null ? "No payment yet" : paymentSummary.getPaymentMethod().getDisplayName();
    }

    private String paymentStatus(PaymentSummaryDto paymentSummary) {
        return paymentSummary == null ? "No payment yet" : paymentSummary.getPaymentStatus().getDisplayName();
    }

    private Object value(Object value) {
        return Optional.ofNullable(value).orElse("");
    }
}
