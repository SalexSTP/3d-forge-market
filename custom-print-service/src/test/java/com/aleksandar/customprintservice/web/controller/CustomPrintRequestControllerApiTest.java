package com.aleksandar.customprintservice.web.controller;

import com.aleksandar.customprintservice.CustomPrintRequestTestData;
import com.aleksandar.customprintservice.model.dto.CreateCustomPrintRequestDto;
import com.aleksandar.customprintservice.model.enums.CustomPrintRequestStatus;
import com.aleksandar.customprintservice.repository.CustomPrintRequestRepository;
import com.aleksandar.customprintservice.service.CustomPrintRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomPrintRequestControllerApiTest {

    private static final String BASE_PATH = "/api/custom-print-requests";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomPrintRequestService service;

    @Autowired
    private CustomPrintRequestRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void postCreateRequestReturnsCreatedForValidRequest() throws Exception {
        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CustomPrintRequestTestData.validCreateDto())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.customerId").value(CustomPrintRequestTestData.CUSTOMER_ID.toString()))
                .andExpect(jsonPath("$.adminAttentionRequired").value(false))
                .andExpect(jsonPath("$.customerResponseReminderRequired").value(false));
    }

    @Test
    void invalidCreateRequestReturnsValidationErrorWithoutInternalDetails() throws Exception {
        CreateCustomPrintRequestDto invalidDto = new CreateCustomPrintRequestDto(
                null,
                "ab",
                "not-an-email",
                "Tiny",
                "Too short",
                "P",
                "",
                "short",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                null
        );

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed."))
                .andExpect(jsonPath("$.errors.customerId").value("Customer ID is required."))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("org.hibernate"))));
    }

    @Test
    void getCustomerListReturnsVisibleCustomerRequests() throws Exception {
        UUID requestId = service.createRequest(CustomPrintRequestTestData.validCreateDto()).id();

        mockMvc.perform(get(BASE_PATH + "/customer/{customerId}", CustomPrintRequestTestData.CUSTOMER_ID)
                        .param("keyword", "phone")
                        .param("status", "PENDING_REVIEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(requestId.toString()))
                .andExpect(jsonPath("$[0].title").value("Custom phone stand"));
    }

    @Test
    void getCustomerDetailsReturnsOwnedRequest() throws Exception {
        UUID requestId = service.createRequest(CustomPrintRequestTestData.validCreateDto()).id();

        mockMvc.perform(get(BASE_PATH + "/customer/{customerId}/{requestId}", CustomPrintRequestTestData.CUSTOMER_ID, requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requestId.toString()))
                .andExpect(jsonPath("$.description").value("Please print a sturdy adjustable phone stand for my desk."));
    }

    @Test
    void putCustomerEditUpdatesPendingRequest() throws Exception {
        UUID requestId = service.createRequest(CustomPrintRequestTestData.validCreateDto()).id();

        mockMvc.perform(put(BASE_PATH + "/customer/{customerId}/{requestId}", CustomPrintRequestTestData.CUSTOMER_ID, requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CustomPrintRequestTestData.validUpdateDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated phone stand"))
                .andExpect(jsonPath("$.material").value("PETG"));
    }

    @Test
    void editingAfterOfferSentReturnsFriendlyOperationError() throws Exception {
        UUID requestId = createOfferSentRequest();

        mockMvc.perform(put(BASE_PATH + "/customer/{customerId}/{requestId}", CustomPrintRequestTestData.CUSTOMER_ID, requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CustomPrintRequestTestData.validUpdateDto())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Only pending custom print requests can be edited."))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void adminSendsOfferAndCustomerAcceptsOffer() throws Exception {
        UUID requestId = service.createRequest(CustomPrintRequestTestData.validCreateDto()).id();

        mockMvc.perform(put(BASE_PATH + "/{requestId}/offer", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CustomPrintRequestTestData.validOfferDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OFFER_SENT"))
                .andExpect(jsonPath("$.quotedPrice").value(49.99));

        mockMvc.perform(put(BASE_PATH + "/customer/{customerId}/{requestId}/accept", CustomPrintRequestTestData.CUSTOMER_ID, requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.acceptedOn").exists());
    }

    @Test
    void adminProgressesFulfillmentStatus() throws Exception {
        UUID requestId = createAcceptedRequest();

        mockMvc.perform(put(BASE_PATH + "/{requestId}/fulfillment-status", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CustomPrintRequestTestData.fulfillmentDto(CustomPrintRequestStatus.PRINTING))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PRINTING"))
                .andExpect(jsonPath("$.printingStartedOn").exists());
    }

    @Test
    void invalidOperationReturnsFriendlyJsonError() throws Exception {
        UUID requestId = createAcceptedRequest();

        mockMvc.perform(put(BASE_PATH + "/customer/{customerId}/{requestId}/cancel", CustomPrintRequestTestData.CUSTOMER_ID, requestId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Only pending, offer-sent, or change-requested custom print requests can be cancelled."))
                .andExpect(jsonPath("$.errors").isMap())
                .andExpect(content().string(not(containsString("CustomPrintRequestOperationNotAllowedException"))));
    }

    @Test
    void nonExistingRequestReturnsFriendlyNotFoundJsonError() throws Exception {
        UUID missingId = UUID.fromString("99999999-9999-9999-9999-999999999999");

        mockMvc.perform(get(BASE_PATH + "/{requestId}", missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Custom print request was not found."))
                .andExpect(jsonPath("$.errors").isMap())
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("SQL"))));
    }

    @Test
    void malformedRequestIdReturnsFriendlyBadRequestJsonError() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request parameter."))
                .andExpect(content().string(not(containsString("MethodArgumentTypeMismatchException"))));
    }

    private UUID createOfferSentRequest() {
        UUID requestId = service.createRequest(CustomPrintRequestTestData.validCreateDto()).id();
        return service.sendOffer(requestId, CustomPrintRequestTestData.validOfferDto()).id();
    }

    private UUID createAcceptedRequest() {
        UUID requestId = createOfferSentRequest();
        return service.acceptOffer(requestId, CustomPrintRequestTestData.CUSTOMER_ID).id();
    }
}
