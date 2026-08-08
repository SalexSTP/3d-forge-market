package com.aleksandar.threedforgemarket.service.review;

import com.aleksandar.threedforgemarket.exception.review.ReviewAlreadyExistsException;
import com.aleksandar.threedforgemarket.exception.review.ReviewEligibilityNotMetException;
import com.aleksandar.threedforgemarket.exception.review.ReviewNotFoundException;
import com.aleksandar.threedforgemarket.model.entity.CustomerOrder;
import com.aleksandar.threedforgemarket.model.entity.Product;
import com.aleksandar.threedforgemarket.model.entity.Review;
import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.enums.order.OrderStatus;
import com.aleksandar.threedforgemarket.model.review.ReviewFormDto;
import com.aleksandar.threedforgemarket.model.review.ReviewListItemDto;
import com.aleksandar.threedforgemarket.repository.order.CustomerOrderRepository;
import com.aleksandar.threedforgemarket.repository.product.ProductRepository;
import com.aleksandar.threedforgemarket.repository.review.ReviewRepository;
import com.aleksandar.threedforgemarket.repository.user.UserRepository;
import com.aleksandar.threedforgemarket.testdata.OrderTestData;
import com.aleksandar.threedforgemarket.testdata.ProductTestData;
import com.aleksandar.threedforgemarket.testdata.ReviewTestData;
import com.aleksandar.threedforgemarket.testdata.UserTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ReviewServiceTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    private User customer;
    private User otherCustomer;
    private Product product;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        customerOrderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        customer = userRepository.save(UserTestData.customer("reviewer"));
        otherCustomer = userRepository.save(UserTestData.customer("otherreviewer"));
        product = productRepository.save(ProductTestData.product("Reviewed Product"));
        CustomerOrder deliveredOrder = OrderTestData.order(customer, product, OrderStatus.DELIVERED);
        customerOrderRepository.save(deliveredOrder);
    }

    @Test
    void customerCreatesReviewForDeliveredProduct() {
        reviewService.createReview(customer.getId(), ReviewTestData.reviewForm(product.getId()));

        assertThat(reviewRepository.findAll())
                .singleElement()
                .satisfies(review -> {
                    assertThat(review.getAuthor().getId()).isEqualTo(customer.getId());
                    assertThat(review.getProduct().getId()).isEqualTo(product.getId());
                    assertThat(review.getRating()).isEqualTo(5);
                });
    }

    @Test
    void customerUpdatesOwnReview() {
        Review review = reviewRepository.save(ReviewTestData.review(customer, product));
        ReviewFormDto form = ReviewTestData.reviewForm(product.getId());
        form.setRating(2);
        form.setComment("Updated review comment.");

        UUID productId = reviewService.updateReview(review.getId(), customer.getId(), form);

        assertThat(productId).isEqualTo(product.getId());
        assertThat(reviewRepository.findById(review.getId()).orElseThrow().getRating()).isEqualTo(2);
    }

    @Test
    void customerDeletesOwnReview() {
        Review review = reviewRepository.save(ReviewTestData.review(customer, product));

        UUID productId = reviewService.deleteReviewByAuthor(review.getId(), customer.getId());

        assertThat(productId).isEqualTo(product.getId());
        assertThat(reviewRepository.existsById(review.getId())).isFalse();
    }

    @Test
    void adminDeletesReview() {
        Review review = reviewRepository.save(ReviewTestData.review(customer, product));

        reviewService.deleteReviewAsAdmin(review.getId());

        assertThat(reviewRepository.existsById(review.getId())).isFalse();
    }

    @Test
    void customerCannotUpdateOrDeleteAnotherCustomersReview() {
        Review review = reviewRepository.save(ReviewTestData.review(customer, product));

        assertThatThrownBy(() -> reviewService.updateReview(review.getId(), otherCustomer.getId(), ReviewTestData.reviewForm(product.getId())))
                .isInstanceOf(ReviewNotFoundException.class);
        assertThatThrownBy(() -> reviewService.deleteReviewByAuthor(review.getId(), otherCustomer.getId()))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    void reviewListMappingReturnsExpectedFields() {
        Review review = reviewRepository.save(ReviewTestData.review(customer, product));

        List<ReviewListItemDto> reviews = reviewService.getReviewsForProduct(product.getId());

        assertThat(reviews)
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getId()).isEqualTo(review.getId());
                    assertThat(item.getProductName()).isEqualTo(product.getName());
                    assertThat(item.getAuthorUsername()).isEqualTo(customer.getUsername());
                    assertThat(item.getRating()).isEqualTo(4);
                });
    }

    @Test
    void canCustomerReviewRequiresCustomerDeliveredOrderAndNoExistingReview() {
        assertThat(reviewService.canCustomerReview(customer.getId(), product.getId())).isTrue();

        reviewRepository.save(ReviewTestData.review(customer, product));

        assertThat(reviewService.canCustomerReview(customer.getId(), product.getId())).isFalse();
        assertThat(reviewService.canCustomerReview(null, product.getId())).isFalse();
        assertThat(reviewService.canCustomerReview(otherCustomer.getId(), product.getId())).isFalse();
    }

    @Test
    void createReviewRejectsDuplicateOrMissingDeliveredOrder() {
        reviewRepository.save(ReviewTestData.review(customer, product));

        assertThatThrownBy(() -> reviewService.createReview(customer.getId(), ReviewTestData.reviewForm(product.getId())))
                .isInstanceOf(ReviewAlreadyExistsException.class);
        assertThatThrownBy(() -> reviewService.createReview(otherCustomer.getId(), ReviewTestData.reviewForm(product.getId())))
                .isInstanceOf(ReviewEligibilityNotMetException.class);
    }
}
