package com.aleksandar.threedforgemarket.service.report;

import com.aleksandar.threedforgemarket.exception.customprint.CustomPrintServiceUnavailableException;
import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestListItemClientDto;
import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestStatus;
import com.aleksandar.threedforgemarket.model.dto.customprint.CustomPrintSearchRequest;
import com.aleksandar.threedforgemarket.model.dto.payment.PaymentSummaryDto;
import com.aleksandar.threedforgemarket.model.entity.CustomerOrder;
import com.aleksandar.threedforgemarket.model.entity.Product;
import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.enums.order.OrderStatus;
import com.aleksandar.threedforgemarket.model.enums.payment.PaymentMethod;
import com.aleksandar.threedforgemarket.model.enums.payment.PaymentStatus;
import com.aleksandar.threedforgemarket.model.enums.payment.PaymentTargetType;
import com.aleksandar.threedforgemarket.model.enums.product.PrintMaterial;
import com.aleksandar.threedforgemarket.model.enums.product.ProductCategory;
import com.aleksandar.threedforgemarket.model.enums.user.UserRole;
import com.aleksandar.threedforgemarket.repository.order.CustomerOrderRepository;
import com.aleksandar.threedforgemarket.repository.review.ReviewRepository;
import com.aleksandar.threedforgemarket.repository.user.UserRepository;
import com.aleksandar.threedforgemarket.service.customprint.CustomPrintRequestService;
import com.aleksandar.threedforgemarket.service.payment.PaymentService;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminReportServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private CustomPrintRequestService customPrintRequestService;

    @Mock
    private PaymentService paymentService;

    private AdminReportService adminReportService;

    @BeforeEach
    void setUp() {
        adminReportService = new AdminReportService(
                userRepository,
                customerOrderRepository,
                reviewRepository,
                customPrintRequestService,
                paymentService,
                new ExcelReportWriter()
        );
    }

    @Test
    void usersWorkbookContainsExpectedSheetHeadersAndNoPasswordHash() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("report_user")
                .email("report@example.com")
                .password("$2a$10$secret-password-hash")
                .role(UserRole.CUSTOMER)
                .active(true)
                .createdOn(LocalDateTime.of(2026, 8, 9, 10, 0))
                .lastLoginOn(LocalDateTime.of(2026, 8, 9, 11, 0))
                .build();

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(customerOrderRepository.countByCustomer_Id(userId)).thenReturn(2L);
        when(reviewRepository.countByAuthor_Id(userId)).thenReturn(1L);

        try (XSSFWorkbook workbook = workbook(adminReportService.generateUsersReport())) {
            Sheet sheet = workbook.getSheet("Users");

            assertThat(sheet).isNotNull();
            assertThat(rowText(sheet, 0)).contains("User ID", "Username", "Email", "Orders count", "Reviews count");
            assertThat(rowText(sheet, 1)).contains(userId.toString(), "report_user", "report@example.com", "CUSTOMER", "2", "1");
            assertThat(sheetText(sheet)).doesNotContain("$2a$10$secret-password-hash");
        }
    }

    @Test
    void ordersWorkbookContainsExpectedSheetHeadersAndPaymentValues() throws Exception {
        UUID orderId = UUID.randomUUID();
        CustomerOrder order = CustomerOrder.builder()
                .id(orderId)
                .customer(user("buyer", "buyer@example.com"))
                .product(product("Modern Simple Hook"))
                .quantity(2)
                .totalPrice(new BigDecimal("24.00"))
                .status(OrderStatus.CONFIRMED)
                .createdOn(LocalDateTime.of(2026, 8, 9, 12, 0))
                .updatedOn(LocalDateTime.of(2026, 8, 9, 13, 0))
                .deliveryAddress("Sofia, Bulgaria")
                .customerNote("Leave at reception")
                .build();

        when(customerOrderRepository.findAllForAdminOrderedByStatus()).thenReturn(List.of(order));
        when(paymentService.getLatestPaymentSummary(PaymentTargetType.PRODUCT_ORDER, orderId))
                .thenReturn(Optional.of(payment(PaymentMethod.STRIPE_CHECKOUT, PaymentStatus.PAID)));

        try (XSSFWorkbook workbook = workbook(adminReportService.generateOrdersReport())) {
            Sheet sheet = workbook.getSheet("Orders");

            assertThat(sheet).isNotNull();
            assertThat(rowText(sheet, 0)).contains("Order ID", "Total EUR", "Payment method", "Payment status");
            assertThat(rowText(sheet, 1)).contains(orderId.toString(), "Modern Simple Hook", "Stripe Checkout", "Paid");
        }
    }

    @Test
    void customPrintsWorkbookContainsExpectedSheetHeadersAndPaymentValues() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(customPrintRequestService.getAllRequestsForAdmin(any(CustomPrintSearchRequest.class)))
                .thenReturn(List.of(customPrintRequest(
                        requestId,
                        CustomPrintRequestStatus.ACCEPTED,
                        false,
                        false
                )));
        when(paymentService.getLatestPaymentSummary(
                eq(PaymentTargetType.CUSTOM_PRINT_REQUEST),
                eq(requestId),
                eq(false)
        )).thenReturn(Optional.of(payment(PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PENDING_CASH_ON_DELIVERY)));

        try (XSSFWorkbook workbook = workbook(adminReportService.generateCustomPrintsReport())) {
            Sheet sheet = workbook.getSheet("Custom prints");

            assertThat(sheet).isNotNull();
            assertThat(rowText(sheet, 0)).contains("Request ID", "Quoted price EUR", "Payment method", "Payment status");
            assertThat(rowText(sheet, 1)).contains(requestId.toString(), "Custom enclosure", "Cash on delivery", "Cash on delivery pending");
        }
    }

    @Test
    void customPrintsWorkbookUsesActualMaintenanceFlagsInsteadOfStatusDerivedValues() throws Exception {
        UUID pendingReviewRequestId = UUID.randomUUID();
        UUID offerSentRequestId = UUID.randomUUID();
        UUID flaggedRequestId = UUID.randomUUID();
        when(customPrintRequestService.getAllRequestsForAdmin(any(CustomPrintSearchRequest.class)))
                .thenReturn(List.of(
                        customPrintRequest(
                                pendingReviewRequestId,
                                CustomPrintRequestStatus.PENDING_REVIEW,
                                false,
                                false
                        ),
                        customPrintRequest(
                                offerSentRequestId,
                                CustomPrintRequestStatus.OFFER_SENT,
                                false,
                                false
                        ),
                        customPrintRequest(
                                flaggedRequestId,
                                CustomPrintRequestStatus.ACCEPTED,
                                true,
                                true
                        )
                ));

        try (XSSFWorkbook workbook = workbook(adminReportService.generateCustomPrintsReport())) {
            Sheet sheet = workbook.getSheet("Custom prints");

            assertThat(sheet.getRow(1).getCell(14).getBooleanCellValue()).isFalse();
            assertThat(sheet.getRow(1).getCell(15).getBooleanCellValue()).isFalse();
            assertThat(sheet.getRow(2).getCell(14).getBooleanCellValue()).isFalse();
            assertThat(sheet.getRow(2).getCell(15).getBooleanCellValue()).isFalse();
            assertThat(sheet.getRow(3).getCell(14).getBooleanCellValue()).isTrue();
            assertThat(sheet.getRow(3).getCell(15).getBooleanCellValue()).isTrue();
        }
    }

    @Test
    void fullWorkbookContainsAllExpectedSheets() throws Exception {
        when(userRepository.findAll()).thenReturn(List.of());
        when(customerOrderRepository.findAllForAdminOrderedByStatus()).thenReturn(List.of());
        when(customPrintRequestService.getAllRequestsForAdmin(any(CustomPrintSearchRequest.class))).thenReturn(List.of());

        try (XSSFWorkbook workbook = workbook(adminReportService.generateFullReport())) {
            assertThat(workbook.getSheet("Users")).isNotNull();
            assertThat(workbook.getSheet("Orders")).isNotNull();
            assertThat(workbook.getSheet("Custom prints")).isNotNull();
        }
    }

    @Test
    void fullWorkbookContainsCustomPrintUnavailableMessageSheet() throws Exception {
        when(userRepository.findAll()).thenReturn(List.of());
        when(customerOrderRepository.findAllForAdminOrderedByStatus()).thenReturn(List.of());
        when(customPrintRequestService.getAllRequestsForAdmin(any(CustomPrintSearchRequest.class)))
                .thenThrow(new CustomPrintServiceUnavailableException());

        try (XSSFWorkbook workbook = workbook(adminReportService.generateFullReport())) {
            Sheet sheet = workbook.getSheet("Custom prints");

            assertThat(sheet).isNotNull();
            assertThat(rowText(sheet, 0)).contains("Custom print service is currently unavailable.");
        }
    }

    private XSSFWorkbook workbook(byte[] bytes) throws Exception {
        return (XSSFWorkbook) WorkbookFactory.create(new ByteArrayInputStream(bytes));
    }

    private String rowText(Sheet sheet, int rowIndex) {
        DataFormatter dataFormatter = new DataFormatter();
        StringBuilder text = new StringBuilder();
        sheet.getRow(rowIndex).forEach(cell -> text.append(dataFormatter.formatCellValue(cell)).append(" | "));
        return text.toString();
    }

    private String sheetText(Sheet sheet) {
        StringBuilder text = new StringBuilder();
        for (int row = 0; row <= sheet.getLastRowNum(); row++) {
            if (sheet.getRow(row) != null) {
                text.append(rowText(sheet, row));
            }
        }
        return text.toString();
    }

    private User user(String username, String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .email(email)
                .role(UserRole.CUSTOMER)
                .active(true)
                .createdOn(LocalDateTime.of(2026, 8, 9, 9, 0))
                .build();
    }

    private Product product(String name) {
        return Product.builder()
                .id(UUID.randomUUID())
                .name(name)
                .description("Description")
                .price(new BigDecimal("12.00"))
                .imageUrl("https://example.com/image.png")
                .estimatedPrintTimeMinutes(120)
                .widthCm(BigDecimal.ONE)
                .heightCm(BigDecimal.ONE)
                .depthCm(BigDecimal.ONE)
                .weightGrams(BigDecimal.ONE)
                .productCategory(ProductCategory.DECORATION)
                .material(PrintMaterial.PLA)
                .colorDescription("Black")
                .available(true)
                .createdOn(LocalDateTime.of(2026, 8, 9, 9, 0))
                .updatedOn(LocalDateTime.of(2026, 8, 9, 9, 0))
                .build();
    }

    private PaymentSummaryDto payment(PaymentMethod method, PaymentStatus status) {
        return PaymentSummaryDto.builder()
                .paymentMethod(method)
                .paymentStatus(status)
                .amount(new BigDecimal("24.00"))
                .currency("eur")
                .build();
    }

    private CustomPrintRequestListItemClientDto customPrintRequest(
            UUID requestId,
            CustomPrintRequestStatus status,
            Boolean adminAttentionRequired,
            Boolean customerResponseReminderRequired
    ) {
        return new CustomPrintRequestListItemClientDto(
                requestId,
                UUID.randomUUID(),
                "custom_customer",
                "custom@example.com",
                "Custom enclosure",
                "PLA",
                "Black",
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                1,
                null,
                "Sofia",
                status,
                new BigDecimal("35.00"),
                180,
                "Ready to print",
                null,
                null,
                LocalDateTime.of(2026, 8, 9, 10, 0),
                LocalDateTime.of(2026, 8, 9, 11, 0),
                LocalDateTime.of(2026, 8, 9, 12, 0),
                null,
                null,
                LocalDateTime.of(2026, 8, 9, 13, 0),
                null,
                null,
                null,
                adminAttentionRequired,
                LocalDateTime.of(2026, 8, 9, 14, 0),
                customerResponseReminderRequired,
                LocalDateTime.of(2026, 8, 9, 15, 0)
        );
    }
}
