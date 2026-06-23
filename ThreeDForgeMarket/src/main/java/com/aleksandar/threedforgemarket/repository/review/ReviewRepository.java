package com.aleksandar.threedforgemarket.repository.review;

import com.aleksandar.threedforgemarket.model.entity.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    @EntityGraph(attributePaths = {"author", "product"})
    List<Review> findAllByProduct_IdOrderByRatingDescCreatedOnDesc(
            UUID productId
    );

    @EntityGraph(attributePaths = {"author", "product"})
    List<Review> findAllByOrderByCreatedOnDesc();

    Optional<Review> findByIdAndAuthor_Id(
            UUID reviewId,
            UUID authorId
    );

    boolean existsByAuthor_IdAndProduct_Id(
            UUID authorId,
            UUID productId
    );
}
