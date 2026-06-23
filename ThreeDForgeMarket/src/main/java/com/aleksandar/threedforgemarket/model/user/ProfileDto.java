package com.aleksandar.threedforgemarket.model.user;

import com.aleksandar.threedforgemarket.model.enums.user.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProfileDto {
    private final String username;
    private final String email;
    private final UserRole role;
    private final LocalDateTime createdOn;
    private final LocalDateTime lastLoginOn;
}
