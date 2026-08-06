package com.aleksandar.threedforgemarket.model.user;

import com.aleksandar.threedforgemarket.model.enums.user.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRoleRequest {
    @NotNull(message = "Please choose a role.")
    private UserRole role;
}
