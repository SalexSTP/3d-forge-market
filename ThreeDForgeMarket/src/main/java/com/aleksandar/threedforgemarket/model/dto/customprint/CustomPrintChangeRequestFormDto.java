package com.aleksandar.threedforgemarket.model.dto.customprint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomPrintChangeRequestFormDto {
    @NotBlank(message = "Change request message is required.")
    @Size(min = 5, max = 1000, message = "Change request message must be between 5 and 1000 characters.")
    private String customerMessage;
}
