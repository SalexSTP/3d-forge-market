package com.aleksandar.threedforgemarket.model.user;

import com.aleksandar.threedforgemarket.model.enums.user.UserRole;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class AdminUserSearchRequest {
    private String keyword;
    private UserRole role;
    private Boolean active;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate lastLoginFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate lastLoginTo;
}
