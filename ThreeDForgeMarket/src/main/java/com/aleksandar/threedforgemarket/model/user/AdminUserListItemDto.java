package com.aleksandar.threedforgemarket.model.user;

import com.aleksandar.threedforgemarket.model.enums.user.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
public class AdminUserListItemDto {
    private final UUID id;
    private final String username;
    private final String email;
    private final UserRole role;
    private final boolean active;
    private final LocalDateTime createdOn;
    private final LocalDateTime lastLoginOn;
    private final LocalDateTime deactivatedOn;
    private final boolean currentUser;
}
