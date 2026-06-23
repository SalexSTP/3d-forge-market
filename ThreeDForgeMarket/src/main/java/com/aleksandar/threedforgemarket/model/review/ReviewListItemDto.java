package com.aleksandar.threedforgemarket.model.review;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ReviewListItemDto {
    private final UUID id;

    private final UUID productId;
    private final String productName;

    private final UUID authorId;
    private final String authorUsername;

    private final Integer rating;
    private final String comment;

    private final LocalDateTime createdOn;
}
