package com.aleksandar.threedforgemarket.web.controller.review;

import com.aleksandar.threedforgemarket.exception.review.ReviewNotFoundException;
import com.aleksandar.threedforgemarket.service.review.ReviewService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/admin/reviews")
public class AdminReviewController {
    private final ReviewService reviewService;

    public AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ModelAndView getReviewModerationPage() {
        ModelAndView modelAndView = new ModelAndView("admin/reviews");

        modelAndView.addObject(
                "reviews",
                reviewService.getAllReviewsForAdmin()
        );

        return modelAndView;
    }

    @DeleteMapping("/{id}")
    public ModelAndView deleteReview(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            reviewService.deleteReviewAsAdmin(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Review was deleted successfully."
            );

        } catch (ReviewNotFoundException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return new ModelAndView("redirect:/admin/reviews");
    }
}
