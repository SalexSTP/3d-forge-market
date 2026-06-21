package com.aleksandar.threedforgemarket.web.controller.product;

import com.aleksandar.threedforgemarket.model.enums.product.ProductCategory;
import com.aleksandar.threedforgemarket.service.product.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
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

        modelAndView.addObject("categories",  ProductCategory.values());
        modelAndView.addObject("search", search);
        modelAndView.addObject("selectedCategory", category);

        return modelAndView;
    }

    @GetMapping("/{id}")
    public ModelAndView getProductDetailsPage(@PathVariable UUID id) {
        ModelAndView modelAndView = new ModelAndView("product/details");

        modelAndView.addObject(
                "product",
                productService.getAvailableProductDetails(id)
        );

        //TODO: Add Reviews
        modelAndView.addObject("reviews", List.of());
        modelAndView.addObject("canReview", false);

        return modelAndView;
    }
}
