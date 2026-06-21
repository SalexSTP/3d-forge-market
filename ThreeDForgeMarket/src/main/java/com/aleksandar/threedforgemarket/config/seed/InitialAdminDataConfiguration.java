package com.aleksandar.threedforgemarket.config.seed;

import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.enums.user.UserRole;
import com.aleksandar.threedforgemarket.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

@Configuration
public class InitialAdminDataConfiguration {
    @Bean
    public CommandLineRunner seedAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${ADMIN_USERNAME:admin}") String adminUsername,
            @Value("${ADMIN_EMAIL:admin@3dforgemarket.local}") String adminEmail,
            @Value("${ADMIN_PASSWORD:}") String adminPassword
    ) {
        return arguments -> {
            if (!StringUtils.hasText(adminPassword)) {
                return;
            }

            boolean usernameExists = userRepository.existsByUsername(adminUsername);
            boolean emailExists = userRepository.existsByEmail(adminEmail);

            if (usernameExists || emailExists) {
                return;
            }

            User admin = User.builder()
                    .username(adminUsername)
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(UserRole.ADMIN)
                    .build();

            userRepository.save(admin);
        };
    }
}
