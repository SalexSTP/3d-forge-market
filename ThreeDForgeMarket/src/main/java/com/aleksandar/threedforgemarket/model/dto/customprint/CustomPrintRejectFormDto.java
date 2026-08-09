package com.aleksandar.threedforgemarket.model.dto.customprint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomPrintRejectFormDto {
    @NotBlank(message = "Rejection message is required.")
    @Size(min = 5, max = 1000, message = "Rejection message must be between 5 and 1000 characters.")
    private String adminMessage;
}
