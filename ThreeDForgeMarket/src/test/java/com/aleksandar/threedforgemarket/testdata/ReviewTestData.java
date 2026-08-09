package com.aleksandar.threedforgemarket.testdata;

import com.aleksandar.threedforgemarket.model.entity.Product;
import com.aleksandar.threedforgemarket.model.entity.Review;
import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.review.ReviewFormDto;

import java.util.UUID;

public final class ReviewTestData {

    private ReviewTestData() {
    }

    public static ReviewFormDto reviewForm(UUID productId) {
        ReviewFormDto form = new ReviewFormDto();
        form.setProductId(productId);
        form.setRating(5);
        form.setComment("Excellent printed product.");
        return form;
    }

    public static Review review(User author, Product product) {
        return Review.builder()
                .author(author)
                .product(product)
                .rating(4)
                .comment("Very solid printed item.")
                .build();
    }
}
