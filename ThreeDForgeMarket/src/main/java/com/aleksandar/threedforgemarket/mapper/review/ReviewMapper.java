package com.aleksandar.threedforgemarket.mapper.review;

import com.aleksandar.threedforgemarket.model.entity.Product;
import com.aleksandar.threedforgemarket.model.entity.Review;
import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.review.ReviewFormDto;
import com.aleksandar.threedforgemarket.model.review.ReviewListItemDto;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public Review toEntity(
            ReviewFormDto reviewForm,
            Product product,
            User author
    ) {
        return Review.builder()
                .product(product)
                .author(author)
                .rating(reviewForm.getRating())
                .comment(reviewForm.getComment().strip())
                .build();
    }

    public ReviewFormDto toFormDto(Review review) {
        ReviewFormDto reviewForm = new ReviewFormDto();

        reviewForm.setId(review.getId());
        reviewForm.setProductId(review.getProduct().getId());
        reviewForm.setRating(review.getRating());
        reviewForm.setComment(review.getComment());

        return reviewForm;
    }

    public ReviewListItemDto toListItemDto(Review review) {
        return ReviewListItemDto.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .authorId(review.getAuthor().getId())
                .authorUsername(review.getAuthor().getUsername())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdOn(review.getCreatedOn())
                .build();
    }

    public void updateEntity(
            Review review,
            ReviewFormDto reviewForm
    ) {
        review.setRating(reviewForm.getRating());
        review.setComment(reviewForm.getComment().strip());
    }
}
