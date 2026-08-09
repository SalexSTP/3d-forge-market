package com.aleksandar.threedforgemarket.model.dto.customprint;

import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomPrintFulfillmentStatusFormDto {
    @NotNull(message = "Fulfillment status is required.")
    private CustomPrintRequestStatus status;
}
