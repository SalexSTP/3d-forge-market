package com.aleksandar.threedforgemarket.config;

import com.aleksandar.threedforgemarket.security.MarketplaceAuthenticationFailureHandler;
import com.aleksandar.threedforgemarket.security.MarketplaceAuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
    private final MarketplaceAuthenticationFailureHandler authenticationFailureHandler;
    private final MarketplaceAuthenticationSuccessHandler authenticationSuccessHandler;

    public SecurityConfiguration(
            MarketplaceAuthenticationFailureHandler authenticationFailureHandler,
            MarketplaceAuthenticationSuccessHandler authenticationSuccessHandler
    ) {
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        return http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(
                                "/",
                                "/auth/**",
                                "/products",
                                "/products/*",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico",
                                "/error"
                        ).permitAll()
                        .requestMatchers("/profile", "/profile/**").authenticated()
                        .requestMatchers("/custom-prints", "/custom-prints/**").hasRole("CUSTOMER")
                        .requestMatchers("/admin/custom-prints", "/admin/custom-prints/**").hasRole("ADMIN")
                        .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                        .requestMatchers("/orders", "/orders/**").hasRole("CUSTOMER")
                        .requestMatchers("/reviews", "/reviews/**").hasRole("CUSTOMER")
                        .anyRequest().authenticated()
                )
                .formLogin(formLogin -> formLogin
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .usernameParameter("usernameOrEmail")
                        .passwordParameter("password")
                        .failureHandler(authenticationFailureHandler)
                        .successHandler(authenticationSuccessHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessUrl("/auth/login?logout")
                )
                .build();
    }
}
