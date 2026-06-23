package com.aleksandar.threedforgemarket.model.review;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ReviewFormDto {
    private UUID id;

    @NotNull(message = "Product is required.")
    private UUID productId;

    @NotNull(message = "Rating is required.")
    @Min(value = 1, message = "Rating must be at least 1.")
    @Max(value = 5, message = "Rating cannot exceed 5.")
    private Integer rating;

    @NotBlank(message = "Comment is required.")
    @Size(
            min = 10,
            max = 1000,
            message = "Comment must be between 10 and 1000 characters."
    )
    private String comment;
}
