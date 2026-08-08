package com.aleksandar.threedforgemarket.web.controller.customprint;

import com.aleksandar.threedforgemarket.exception.customprint.CustomPrintRequestOperationFailedException;
import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestDetailsClientDto;
import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestStatus;
import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.repository.order.CustomerOrderRepository;
import com.aleksandar.threedforgemarket.repository.product.ProductRepository;
import com.aleksandar.threedforgemarket.repository.review.ReviewRepository;
import com.aleksandar.threedforgemarket.repository.user.UserRepository;
import com.aleksandar.threedforgemarket.service.customprint.CustomPrintRequestService;
import com.aleksandar.threedforgemarket.testdata.CustomPrintClientTestData;
import com.aleksandar.threedforgemarket.testdata.UserTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static com.aleksandar.threedforgemarket.testsecurity.MarketplaceSecurityTestSupport.admin;
import static com.aleksandar.threedforgemarket.testsecurity.MarketplaceSecurityTestSupport.customer;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomPrintMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomPrintRequestService customPrintRequestService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private ProductRepository productRepository;

    private User customer;
    private User admin;
    private UUID requestId;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        customerOrderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        customer = userRepository.save(UserTestData.customer("custom_customer"));
        admin = userRepository.save(UserTestData.admin("custom_admin"));
        requestId = UUID.randomUUID();
    }

    @Test
    void customerCustomPrintListRequiresCustomerRole() throws Exception {
        when(customPrintRequestService.getCustomerRequests(eq(customer.getId()), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/custom-prints"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/auth/login"));

        mockMvc.perform(get("/custom-prints").with(admin(admin.getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/custom-prints").with(customer(customer.getId())))
                .andExpect(status().isOk())
                .andExpect(view().name("custom-print/list"));
    }

    @Test
    void detailsPageShowsEditRequestOnlyForPendingReview() throws Exception {
        when(customPrintRequestService.getCustomerRequestDetails(customer.getId(), requestId))
                .thenReturn(details(CustomPrintRequestStatus.PENDING_REVIEW));

        mockMvc.perform(get("/custom-prints/{id}", requestId).with(customer(customer.getId())))
                .andExpect(status().isOk())
                .andExpect(view().name("custom-print/details"))
                .andExpect(content().string(containsString("Edit request")));
    }

    @Test
    void detailsPageDoesNotShowEditRequestForNonPendingStatuses() throws Exception {
        for (CustomPrintRequestStatus status : List.of(
                CustomPrintRequestStatus.OFFER_SENT,
                CustomPrintRequestStatus.ACCEPTED,
                CustomPrintRequestStatus.PRINTING,
                CustomPrintRequestStatus.READY_FOR_DELIVERY,
                CustomPrintRequestStatus.DELIVERED,
                CustomPrintRequestStatus.REJECTED,
                CustomPrintRequestStatus.CANCELLED
        )) {
            UUID id = UUID.randomUUID();
            when(customPrintRequestService.getCustomerRequestDetails(customer.getId(), id))
                    .thenReturn(CustomPrintClientTestData.details(id, customer.getId(), status));

            mockMvc.perform(get("/custom-prints/{id}", id).with(customer(customer.getId())))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("Edit request"))));
        }
    }

    @Test
    void directAccessToEditPageIsRedirectedForNonPendingStatus() throws Exception {
        when(customPrintRequestService.getEditForm(customer.getId(), requestId))
                .thenThrow(new CustomPrintRequestOperationFailedException("Only pending custom print requests can be edited."));

        mockMvc.perform(get("/custom-prints/{id}/edit", requestId).with(customer(customer.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/custom-prints/" + requestId));
    }

    @Test
    void customerCanCreateEditAndProgressRequestActionsWithCsrf() throws Exception {
        when(customPrintRequestService.getEditForm(customer.getId(), requestId))
                .thenReturn(CustomPrintClientTestData.requestForm());

        mockMvc.perform(post("/custom-prints")
                        .with(customer(customer.getId()))
                        .with(csrf())
                        .params(validRequestParams()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/custom-prints"));

        mockMvc.perform(get("/custom-prints/{id}/edit", requestId).with(customer(customer.getId())))
                .andExpect(status().isOk())
                .andExpect(view().name("custom-print/edit"));

        mockMvc.perform(put("/custom-prints/{id}", requestId)
                        .with(customer(customer.getId()))
                        .with(csrf())
                        .params(validRequestParams()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/custom-prints/" + requestId));

        mockMvc.perform(put("/custom-prints/{id}/cancel", requestId)
                        .with(customer(customer.getId()))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/custom-prints"));

        mockMvc.perform(put("/custom-prints/{id}/accept", requestId)
                        .with(customer(customer.getId()))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/custom-prints/" + requestId));

        mockMvc.perform(put("/custom-prints/{id}/request-changes", requestId)
                        .with(customer(customer.getId()))
                        .with(csrf())
                        .param("customerMessage", "Please adjust the dimensions."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/custom-prints/" + requestId));

        mockMvc.perform(put("/custom-prints/{id}/hide", requestId)
                        .with(customer(customer.getId()))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/custom-prints"));

        verify(customPrintRequestService).createCustomerRequest(eq(customer.getId()), any());
        verify(customPrintRequestService).updateCustomerRequest(eq(customer.getId()), eq(requestId), any());
        verify(customPrintRequestService).cancelCustomerRequest(customer.getId(), requestId);
        verify(customPrintRequestService).acceptOffer(customer.getId(), requestId);
        verify(customPrintRequestService).requestChanges(eq(customer.getId()), eq(requestId), any());
        verify(customPrintRequestService).hideCustomerRequest(customer.getId(), requestId);
    }

    @Test
    void postWithoutCsrfIsRejectedBeforeCustomPrintCreateLogic() throws Exception {
        mockMvc.perform(post("/custom-prints")
                        .with(customer(customer.getId()))
                        .params(validRequestParams()))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidChangeRequestReturnsDetailsViewWithValidationErrors() throws Exception {
        when(customPrintRequestService.getCustomerRequestDetails(customer.getId(), requestId))
                .thenReturn(details(CustomPrintRequestStatus.OFFER_SENT));

        mockMvc.perform(put("/custom-prints/{id}/request-changes", requestId)
                        .with(customer(customer.getId()))
                        .with(csrf())
                        .param("customerMessage", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("custom-print/details"));
    }

    @Test
    void adminCustomPrintPagesRequireAdminRole() throws Exception {
        when(customPrintRequestService.getAllRequestsForAdmin(any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/admin/custom-prints").with(customer(customer.getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/custom-prints").with(admin(admin.getId())))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/custom-prints"));
    }

    @Test
    void adminCanOpenDetailsAndRunCustomPrintActionsWithCsrf() throws Exception {
        when(customPrintRequestService.getRequestDetailsForAdmin(requestId))
                .thenReturn(details(CustomPrintRequestStatus.ACCEPTED));

        mockMvc.perform(get("/admin/custom-prints/{id}", requestId).with(admin(admin.getId())))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/custom-print-details"));

        mockMvc.perform(put("/admin/custom-prints/{id}/offer", requestId)
                        .with(admin(admin.getId()))
                        .with(csrf())
                        .param("quotedPrice", "35.00")
                        .param("estimatedPrintTimeMinutes", "180")
                        .param("adminMessage", "Ready to print.")
                        .param("responseFileUrl", "https://example.com/response.stl"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/custom-prints/" + requestId));

        mockMvc.perform(put("/admin/custom-prints/{id}/reject", requestId)
                        .with(admin(admin.getId()))
                        .with(csrf())
                        .param("adminMessage", "Cannot print this request."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/custom-prints/" + requestId));

        mockMvc.perform(put("/admin/custom-prints/{id}/fulfillment-status", requestId)
                        .with(admin(admin.getId()))
                        .with(csrf())
                        .param("status", CustomPrintRequestStatus.PRINTING.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/custom-prints/" + requestId));

        mockMvc.perform(put("/admin/custom-prints/{id}/archive", requestId)
                        .with(admin(admin.getId()))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/custom-prints"));

        verify(customPrintRequestService).sendOffer(eq(requestId), any());
        verify(customPrintRequestService).rejectRequest(eq(requestId), any());
        verify(customPrintRequestService).updateFulfillmentStatus(eq(requestId), any());
        verify(customPrintRequestService).archiveRequest(requestId);
    }

    @Test
    void invalidAdminOfferReturnsDetailsViewWithValidationErrors() throws Exception {
        when(customPrintRequestService.getRequestDetailsForAdmin(requestId))
                .thenReturn(details(CustomPrintRequestStatus.PENDING_REVIEW));

        mockMvc.perform(put("/admin/custom-prints/{id}/offer", requestId)
                        .with(admin(admin.getId()))
                        .with(csrf())
                        .param("quotedPrice", "")
                        .param("estimatedPrintTimeMinutes", "180"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/custom-print-details"));
    }

    private CustomPrintRequestDetailsClientDto details(CustomPrintRequestStatus status) {
        return CustomPrintClientTestData.details(requestId, customer.getId(), status);
    }

    private org.springframework.util.MultiValueMap<String, String> validRequestParams() {
        org.springframework.util.LinkedMultiValueMap<String, String> params = new org.springframework.util.LinkedMultiValueMap<>();
        params.add("title", "Custom bracket");
        params.add("description", "A durable custom printed bracket for a test fixture.");
        params.add("material", "PLA");
        params.add("colorDescription", "Matte black");
        params.add("widthCm", "10.00");
        params.add("heightCm", "5.00");
        params.add("depthCm", "3.00");
        params.add("quantity", "2");
        params.add("referenceFileUrl", "https://example.com/reference.stl");
        params.add("deliveryAddress", "123 Test Street");
        return params;
    }
}
