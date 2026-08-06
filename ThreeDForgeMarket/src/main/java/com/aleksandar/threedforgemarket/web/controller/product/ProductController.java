package com.aleksandar.threedforgemarket.web.controller.product;

import com.aleksandar.threedforgemarket.model.dto.product.ProductDetailsDto;
import com.aleksandar.threedforgemarket.model.enums.product.ProductCategory;
import com.aleksandar.threedforgemarket.model.enums.user.UserRole;
import com.aleksandar.threedforgemarket.security.MarketplaceUserDetails;
import com.aleksandar.threedforgemarket.service.product.ProductService;
import com.aleksandar.threedforgemarket.service.review.ReviewService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final ReviewService reviewService;

    public ProductController(
            ProductService productService,
            ReviewService reviewService
    ) {
        this.productService = productService;
        this.reviewService = reviewService;
    }

    @GetMapping
    public ModelAndView getCatalogPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ProductCategory category
    ) {
        ModelAndView modelAndView = new ModelAndView("product/catalog");

        modelAndView.addObject(
                "products",
                productService.getAvailableProducts(search, category)
        );

        modelAndView.addObject("categories", ProductCategory.values());
        modelAndView.addObject("search", search);
        modelAndView.addObject("selectedCategory", category);

        return modelAndView;
    }

    @GetMapping("/{id}")
    public ModelAndView getProductDetailsPage(
            @PathVariable UUID id,
            @AuthenticationPrincipal MarketplaceUserDetails currentUser
    ) {
        ProductDetailsDto product = isAdmin(currentUser)
                ? productService.getProductDetailsForAdmin(id)
                : productService.getAvailableProductDetails(id);
        UUID customerId = currentUser != null
                && currentUser.getRole() == UserRole.CUSTOMER
                ? currentUser.getId()
                : null;

        ModelAndView modelAndView = new ModelAndView("product/details");

        modelAndView.addObject("product", product);
        modelAndView.addObject(
                "reviews",
                reviewService.getReviewsForProduct(product.getId())
        );
        modelAndView.addObject(
                "canReview",
                reviewService.canCustomerReview(
                        customerId,
                        product.getId()
                )
        );

        return modelAndView;
    }

    private boolean isAdmin(MarketplaceUserDetails currentUser) {
        return currentUser != null && currentUser.getRole() == UserRole.ADMIN;
    }
}
