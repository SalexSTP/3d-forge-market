package com.aleksandar.threedforgemarket.web.controller.review;

import com.aleksandar.threedforgemarket.exception.product.ProductNotFoundException;
import com.aleksandar.threedforgemarket.exception.review.ReviewAlreadyExistsException;
import com.aleksandar.threedforgemarket.exception.review.ReviewCreationNotAllowedException;
import com.aleksandar.threedforgemarket.exception.review.ReviewEligibilityNotMetException;
import com.aleksandar.threedforgemarket.exception.review.ReviewNotFoundException;
import com.aleksandar.threedforgemarket.model.review.ReviewFormDto;
import com.aleksandar.threedforgemarket.service.review.ReviewService;
import com.aleksandar.threedforgemarket.web.controller.auth.AuthController;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/new")
    public ModelAndView getCreateReviewPage(
            @RequestParam UUID productId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        UUID customerId = getCurrentUserId(session);

        if (!reviewService.canCustomerReview(customerId, productId)) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "You are not eligible to submit a review for this product."
            );

            return redirectToProduct(productId);
        }

        ReviewFormDto reviewForm = new ReviewFormDto();
        reviewForm.setProductId(productId);

        return createReviewFormModelAndView(reviewForm);
    }

    @PostMapping
    public ModelAndView createReview(
            @Valid @ModelAttribute("reviewForm") ReviewFormDto reviewForm,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasFieldErrors("productId")) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please choose a product before submitting a review."
            );

            return new ModelAndView("redirect:/products");
        }

        if (bindingResult.hasErrors()) {
            return createReviewFormModelAndView(reviewForm);
        }

        try {
            reviewService.createReview(
                    getCurrentUserId(session),
                    reviewForm
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Your review was submitted successfully."
            );

            return redirectToProduct(reviewForm.getProductId());

        } catch (ReviewAlreadyExistsException
                 | ReviewEligibilityNotMetException
                 | ReviewCreationNotAllowedException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

            return redirectToProduct(reviewForm.getProductId());

        } catch (ProductNotFoundException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "The requested product was not found."
            );

            return new ModelAndView("redirect:/products");
        }
    }

    @GetMapping("/{id}/edit")
    public ModelAndView getEditReviewPage(
            @PathVariable UUID id,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        try {
            ReviewFormDto reviewForm = reviewService.getReviewFormForCustomer(
                    id,
                    getCurrentUserId(session)
            );

            return editReviewFormModelAndView(reviewForm);

        } catch (ReviewNotFoundException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

            return new ModelAndView("redirect:/products");
        }
    }

    @PutMapping("/{id}")
    public ModelAndView updateReview(
            @PathVariable UUID id,
            @Valid @ModelAttribute("reviewForm") ReviewFormDto reviewForm,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        reviewForm.setId(id);

        if (bindingResult.hasErrors()) {
            return editReviewFormModelAndView(reviewForm);
        }

        try {
            UUID productId = reviewService.updateReview(
                    id,
                    getCurrentUserId(session),
                    reviewForm
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Your review was updated successfully."
            );

            return redirectToProduct(productId);

        } catch (ReviewNotFoundException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

            return new ModelAndView("redirect:/products");
        }
    }

    @DeleteMapping("/{id}")
    public ModelAndView deleteReview(
            @PathVariable UUID id,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        try {
            UUID productId = reviewService.deleteReviewByAuthor(
                    id,
                    getCurrentUserId(session)
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Your review was deleted successfully."
            );

            return redirectToProduct(productId);

        } catch (ReviewNotFoundException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

            return new ModelAndView("redirect:/products");
        }
    }

    private ModelAndView createReviewFormModelAndView(
            ReviewFormDto reviewForm
    ) {
        ModelAndView modelAndView = new ModelAndView("review/create");

        modelAndView.addObject("reviewForm", reviewForm);

        return modelAndView;
    }

    private ModelAndView editReviewFormModelAndView(
            ReviewFormDto reviewForm
    ) {
        ModelAndView modelAndView = new ModelAndView("review/edit");

        modelAndView.addObject("reviewForm", reviewForm);

        return modelAndView;
    }

    private ModelAndView redirectToProduct(UUID productId) {
        return new ModelAndView("redirect:/products/" + productId);
    }

    private UUID getCurrentUserId(HttpSession session) {
        Object sessionUserId = session.getAttribute(
                AuthController.USER_ID_SESSION_ATTRIBUTE
        );

        if (sessionUserId instanceof UUID userId) {
            return userId;
        }

        throw new IllegalStateException("Authenticated user session is required.");
    }
}
