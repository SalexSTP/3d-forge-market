package com.aleksandar.threedforgemarket.testdata;

import com.aleksandar.threedforgemarket.model.dto.auth.RegisterRequest;
import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.enums.user.UserRole;
import com.aleksandar.threedforgemarket.model.user.EditProfileRequest;

public final class UserTestData {

    private UserTestData() {
    }

    public static User customer(String username) {
        return user(username, username + "@example.com", UserRole.CUSTOMER);
    }

    public static User admin(String username) {
        return user(username, username + "@example.com", UserRole.ADMIN);
    }

    public static User inactiveCustomer(String username) {
        User user = customer(username);
        user.setActive(false);
        return user;
    }

    public static RegisterRequest registerRequest(String username) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(username + "@example.com");
        request.setPassword("secret123");
        request.setConfirmPassword("secret123");
        return request;
    }

    public static EditProfileRequest editProfileRequest(String username, String email) {
        EditProfileRequest request = new EditProfileRequest();
        request.setUsername(username);
        request.setEmail(email);
        return request;
    }

    private static User user(String username, String email, UserRole role) {
        return User.builder()
                .username(username)
                .email(email)
                .password("{noop}password")
                .role(role)
                .active(true)
                .build();
    }
}
