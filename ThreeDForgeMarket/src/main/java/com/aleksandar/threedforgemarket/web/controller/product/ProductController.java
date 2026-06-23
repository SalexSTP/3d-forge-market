package com.aleksandar.threedforgemarket.web.controller.product;

import com.aleksandar.threedforgemarket.model.dto.product.ProductDetailsDto;
import com.aleksandar.threedforgemarket.model.enums.user.UserRole;
import com.aleksandar.threedforgemarket.model.enums.product.ProductCategory;
import com.aleksandar.threedforgemarket.service.user.UserService;
import com.aleksandar.threedforgemarket.service.product.ProductService;
import com.aleksandar.threedforgemarket.service.review.ReviewService;
import com.aleksandar.threedforgemarket.web.controller.auth.AuthController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final ReviewService reviewService;
    private final UserService userService;

    public ProductController(
            ProductService productService,
            ReviewService reviewService,
            UserService userService
    ) {
        this.productService = productService;
        this.reviewService = reviewService;
        this.userService = userService;
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
            @SessionAttribute(
                    name = AuthController.USER_ID_SESSION_ATTRIBUTE,
                    required = false
            ) UUID currentUserId
    ) {
        ProductDetailsDto product = isAdmin(currentUserId)
                ? productService.getProductDetailsForAdmin(id)
                : productService.getAvailableProductDetails(id);

        ModelAndView modelAndView = new ModelAndView("product/details");

        modelAndView.addObject("product", product);
        modelAndView.addObject(
                "reviews",
                reviewService.getReviewsForProduct(product.getId())
        );
        modelAndView.addObject(
                "canReview",
                reviewService.canCustomerReview(
                        currentUserId,
                        product.getId()
                )
        );

        return modelAndView;
    }

    private boolean isAdmin(UUID currentUserId) {
        if (currentUserId == null) {
            return false;
        }

        return userService.findById(currentUserId)
                .map(user -> user.getRole() == UserRole.ADMIN)
                .orElse(false);
    }
}
