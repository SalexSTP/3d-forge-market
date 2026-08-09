package com.aleksandar.threedforgemarket.security;

import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.repository.order.CustomerOrderRepository;
import com.aleksandar.threedforgemarket.repository.product.ProductRepository;
import com.aleksandar.threedforgemarket.repository.review.ReviewRepository;
import com.aleksandar.threedforgemarket.repository.user.UserRepository;
import com.aleksandar.threedforgemarket.testdata.UserTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class MarketplaceUserDetailsServiceTest {

    @Autowired
    private MarketplaceUserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        customerOrderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void loadsUserByUsername() {
        userRepository.save(UserTestData.customer("loginuser"));

        MarketplaceUserDetails details = (MarketplaceUserDetails) userDetailsService.loadUserByUsername("loginuser");

        assertThat(details.getUsername()).isEqualTo("loginuser");
        assertThat(details.getEmail()).isEqualTo("loginuser@example.com");
    }

    @Test
    void loadsUserByEmailAndMapsAuthority() {
        userRepository.save(UserTestData.admin("adminlogin"));

        MarketplaceUserDetails details = (MarketplaceUserDetails) userDetailsService.loadUserByUsername("adminlogin@example.com");

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void inactiveUserIsNotEnabled() {
        User inactive = userRepository.save(UserTestData.inactiveCustomer("inactive"));

        MarketplaceUserDetails details = (MarketplaceUserDetails) userDetailsService.loadUserByUsername(inactive.getUsername());

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    void unknownUsernameOrEmailThrows() {
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
