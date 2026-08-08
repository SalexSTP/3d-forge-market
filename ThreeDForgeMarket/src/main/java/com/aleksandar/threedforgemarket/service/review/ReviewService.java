package com.aleksandar.threedforgemarket.service.review;

import com.aleksandar.threedforgemarket.exception.auth.UserNotFoundException;
import com.aleksandar.threedforgemarket.exception.product.ProductNotFoundException;
import com.aleksandar.threedforgemarket.exception.review.ReviewAlreadyExistsException;
import com.aleksandar.threedforgemarket.exception.review.ReviewCreationNotAllowedException;
import com.aleksandar.threedforgemarket.exception.review.ReviewEligibilityNotMetException;
import com.aleksandar.threedforgemarket.exception.review.ReviewNotFoundException;
import com.aleksandar.threedforgemarket.mapper.review.ReviewMapper;
import com.aleksandar.threedforgemarket.model.entity.Product;
import com.aleksandar.threedforgemarket.model.entity.Review;
import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.enums.order.OrderStatus;
import com.aleksandar.threedforgemarket.model.enums.user.UserRole;
import com.aleksandar.threedforgemarket.model.review.ReviewFormDto;
import com.aleksandar.threedforgemarket.model.review.ReviewListItemDto;
import com.aleksandar.threedforgemarket.repository.order.CustomerOrderRepository;
import com.aleksandar.threedforgemarket.repository.product.ProductRepository;
import com.aleksandar.threedforgemarket.repository.review.ReviewRepository;
import com.aleksandar.threedforgemarket.repository.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ReviewService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final ReviewMapper reviewMapper;

    public ReviewService(
            ReviewRepository reviewRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            CustomerOrderRepository customerOrderRepository,
            ReviewMapper reviewMapper
    ) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.reviewMapper = reviewMapper;
    }

    @Transactional(readOnly = true)
    public List<ReviewListItemDto> getReviewsForProduct(UUID productId) {
        return reviewRepository
                .findAllByProduct_IdOrderByRatingDescCreatedOnDesc(productId)
                .stream()
                .map(reviewMapper::toListItemDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewListItemDto> getAllReviewsForAdmin() {
        return reviewRepository.findAllByOrderByCreatedOnDesc()
                .stream()
                .map(reviewMapper::toListItemDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean canCustomerReview(
            UUID customerId,
            UUID productId
    ) {
        if (customerId == null) {
            return false;
        }

        User customer = userRepository.findById(customerId)
                .orElse(null);

        if (customer == null || customer.getRole() != UserRole.CUSTOMER) {
            return false;
        }

        boolean alreadyReviewed = reviewRepository
                .existsByAuthor_IdAndProduct_Id(customerId, productId);

        if (alreadyReviewed) {
            return false;
        }

        return customerOrderRepository
                .existsByCustomer_IdAndProduct_IdAndStatus(
                        customerId,
                        productId,
                        OrderStatus.DELIVERED
                );
    }

    @Transactional
    public void createReview(
            UUID customerId,
            ReviewFormDto reviewForm
    ) {
        User customer = findCustomerById(customerId);

        Product product = productRepository.findById(reviewForm.getProductId())
                .orElseThrow(ProductNotFoundException::new);

        boolean alreadyReviewed = reviewRepository
                .existsByAuthor_IdAndProduct_Id(customerId, product.getId());

        if (alreadyReviewed) {
            throw new ReviewAlreadyExistsException();
        }

        boolean hasDeliveredOrder = customerOrderRepository
                .existsByCustomer_IdAndProduct_IdAndStatus(
                        customerId,
                        product.getId(),
                        OrderStatus.DELIVERED
                );

        if (!hasDeliveredOrder) {
            throw new ReviewEligibilityNotMetException();
        }

        Review review = reviewMapper.toEntity(
                reviewForm,
                product,
                customer
        );

        Review savedReview = reviewRepository.save(review);
        LOGGER.info("Created review with id={} for product id={} by customer id={}",
                savedReview.getId(),
                product.getId(),
                customer.getId());
    }

    @Transactional(readOnly = true)
    public ReviewFormDto getReviewFormForCustomer(
            UUID reviewId,
            UUID customerId
    ) {
        Review review = reviewRepository.findByIdAndAuthor_Id(reviewId, customerId)
                .orElseThrow(ReviewNotFoundException::new);

        return reviewMapper.toFormDto(review);
    }

    @Transactional
    public UUID updateReview(
            UUID reviewId,
            UUID customerId,
            ReviewFormDto reviewForm
    ) {
        Review review = reviewRepository.findByIdAndAuthor_Id(reviewId, customerId)
                .orElseThrow(ReviewNotFoundException::new);

        reviewMapper.updateEntity(review, reviewForm);

        reviewRepository.save(review);
        LOGGER.info("Updated review id={} for product id={} by customer id={}",
                reviewId,
                review.getProduct().getId(),
                customerId);

        return review.getProduct().getId();
    }

    @Transactional
    public UUID deleteReviewByAuthor(
            UUID reviewId,
            UUID customerId
    ) {
        Review review = reviewRepository.findByIdAndAuthor_Id(reviewId, customerId)
                .orElseThrow(ReviewNotFoundException::new);

        UUID productId = review.getProduct().getId();

        reviewRepository.delete(review);
        LOGGER.info("Deleted review id={} for product id={} by customer id={}", reviewId, productId, customerId);

        return productId;
    }

    @Transactional
    public void deleteReviewAsAdmin(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(ReviewNotFoundException::new);

        UUID productId = review.getProduct().getId();
        reviewRepository.delete(review);
        LOGGER.info("Deleted review id={} for product id={} by admin", reviewId, productId);
    }

    private User findCustomerById(UUID customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(UserNotFoundException::new);

        if (customer.getRole() != UserRole.CUSTOMER) {
            throw new ReviewCreationNotAllowedException();
        }

        return customer;
    }
}
