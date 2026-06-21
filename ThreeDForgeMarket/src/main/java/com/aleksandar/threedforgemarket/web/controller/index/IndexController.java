package com.aleksandar.threedforgemarket.web.controller.index;

import com.aleksandar.threedforgemarket.service.product.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class IndexController {
    private final ProductService productService;

    public IndexController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/")
    public ModelAndView getIndexPage() {
        ModelAndView modelAndView = new ModelAndView("index");

        modelAndView.addObject(
                "featuredProducts",
                productService.getFeaturedProducts()
        );

        return modelAndView;
    }
}
