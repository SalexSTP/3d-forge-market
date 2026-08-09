package com.aleksandar.threedforgemarket.service.user;

import com.aleksandar.threedforgemarket.exception.auth.EmailAlreadyExistsException;
import com.aleksandar.threedforgemarket.exception.auth.UserOperationNotAllowedException;
import com.aleksandar.threedforgemarket.exception.auth.UsernameAlreadyExistsException;
import com.aleksandar.threedforgemarket.model.dto.auth.RegisterRequest;
import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.enums.user.UserRole;
import com.aleksandar.threedforgemarket.model.user.EditProfileRequest;
import com.aleksandar.threedforgemarket.repository.order.CustomerOrderRepository;
import com.aleksandar.threedforgemarket.repository.product.ProductRepository;
import com.aleksandar.threedforgemarket.repository.review.ReviewRepository;
import com.aleksandar.threedforgemarket.repository.user.UserRepository;
import com.aleksandar.threedforgemarket.testdata.UserTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    void registrationCreatesActiveCustomerWithHashedPassword() {
        RegisterRequest request = UserTestData.registerRequest("newcustomer");

        userService.register(request);

        User savedUser = userRepository.findByUsername("newcustomer").orElseThrow();
        assertThat(savedUser.getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(savedUser.isActive()).isTrue();
        assertThat(savedUser.getPassword()).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", savedUser.getPassword())).isTrue();
    }

    @Test
    void registrationRejectsDuplicateUsernameAndEmail() {
        userRepository.save(UserTestData.customer("duplicate"));

        assertThatThrownBy(() -> userService.register(UserTestData.registerRequest("duplicate")))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        RegisterRequest duplicateEmail = UserTestData.registerRequest("other");
        duplicateEmail.setEmail("duplicate@example.com");
        assertThatThrownBy(() -> userService.register(duplicateEmail))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void profileUpdateChangesUsernameAndEmail() {
        User user = userRepository.save(UserTestData.customer("profile"));
        EditProfileRequest request = UserTestData.editProfileRequest("updatedprofile", "updated@example.com");

        userService.updateCurrentUserProfile(user.getId(), request);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getUsername()).isEqualTo("updatedprofile");
        assertThat(updated.getEmail()).isEqualTo("updated@example.com");
    }

    @Test
    void adminCanChangeRoleButSelfRoleChangeIsBlocked() {
        User admin = userRepository.save(UserTestData.admin("admin"));
        User customer = userRepository.save(UserTestData.customer("customer"));

        userService.changeUserRole(customer.getId(), UserRole.ADMIN, admin.getId());

        assertThat(userRepository.findById(customer.getId()).orElseThrow().getRole()).isEqualTo(UserRole.ADMIN);
        assertThatThrownBy(() -> userService.changeUserRole(admin.getId(), UserRole.CUSTOMER, admin.getId()))
                .isInstanceOf(UserOperationNotAllowedException.class);
    }

    @Test
    void adminCanDeactivateAndReactivateUserButSelfDeactivationIsBlocked() {
        User admin = userRepository.save(UserTestData.admin("admin"));
        User customer = userRepository.save(UserTestData.customer("customer"));

        userService.deactivateUser(customer.getId(), admin.getId());
        assertThat(userRepository.findById(customer.getId()).orElseThrow().isActive()).isFalse();

        userService.reactivateUser(customer.getId(), admin.getId());
        assertThat(userRepository.findById(customer.getId()).orElseThrow().isActive()).isTrue();

        assertThatThrownBy(() -> userService.deactivateUser(admin.getId(), admin.getId()))
                .isInstanceOf(UserOperationNotAllowedException.class);
    }

    @Test
    void lastActiveAdminProtectionRejectsUnsafeAdminOperations() {
        User admin = userRepository.save(UserTestData.admin("admin"));

        assertThatThrownBy(() -> userService.deactivateUser(admin.getId(), admin.getId()))
                .isInstanceOf(UserOperationNotAllowedException.class);
        assertThatThrownBy(() -> userService.changeUserRole(admin.getId(), UserRole.CUSTOMER, admin.getId()))
                .isInstanceOf(UserOperationNotAllowedException.class);
    }

    @Test
    void profileUpdateRejectsDuplicateUsernameAndEmail() {
        User user = userRepository.save(UserTestData.customer("profile"));
        userRepository.save(UserTestData.customer("taken"));

        assertThatThrownBy(() -> userService.updateCurrentUserProfile(
                user.getId(),
                UserTestData.editProfileRequest("taken", "new-email@example.com")
        )).isInstanceOf(UsernameAlreadyExistsException.class);

        assertThatThrownBy(() -> userService.updateCurrentUserProfile(
                user.getId(),
                UserTestData.editProfileRequest("newname", "taken@example.com")
        )).isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void profileAndAdminListQueriesMapExpectedFields() {
        User admin = userRepository.save(UserTestData.admin("admin"));
        User customer = userRepository.save(UserTestData.customer("listed"));

        userService.updateLastLogin(customer.getId());

        assertThat(userService.getCurrentUserProfile(customer.getId()).getUsername()).isEqualTo("listed");
        assertThat(userService.getEditProfileRequest(customer.getId()).getEmail()).isEqualTo("listed@example.com");
        assertThat(userService.getUsersForAdmin(new com.aleksandar.threedforgemarket.model.user.AdminUserSearchRequest(), admin.getId()))
                .extracting("id")
                .contains(customer.getId(), admin.getId());
    }
}
