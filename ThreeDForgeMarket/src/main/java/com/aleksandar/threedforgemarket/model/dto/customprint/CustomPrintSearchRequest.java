package com.aleksandar.threedforgemarket.model.dto.customprint;

import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class CustomPrintSearchRequest {
    private String keyword;
    private CustomPrintRequestStatus status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdTo;
}
