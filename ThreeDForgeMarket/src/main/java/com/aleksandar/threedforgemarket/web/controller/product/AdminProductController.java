package com.aleksandar.threedforgemarket.web.controller.product;

import com.aleksandar.threedforgemarket.exception.product.ProductNameAlreadyExistsException;
import com.aleksandar.threedforgemarket.model.dto.product.ProductFormDto;
import com.aleksandar.threedforgemarket.model.enums.product.PrintMaterial;
import com.aleksandar.threedforgemarket.model.enums.product.ProductCategory;
import com.aleksandar.threedforgemarket.service.product.ProductService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.UUID;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ModelAndView getProductManagementPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(required = false) PrintMaterial material,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String availability
    ) {
        ModelAndView modelAndView = new ModelAndView("admin/products");

        Boolean available = resolveAvailability(availability);

        modelAndView.addObject(
                "products",
                productService.getAllProductsForAdmin(
                        search,
                        category,
                        material,
                        minPrice,
                        maxPrice,
                        available
                )
        );

        modelAndView.addObject("categories", ProductCategory.values());
        modelAndView.addObject("materials", PrintMaterial.values());
        modelAndView.addObject("search", search);
        modelAndView.addObject("selectedCategory", category);
        modelAndView.addObject("selectedMaterial", material);
        modelAndView.addObject("minPrice", minPrice);
        modelAndView.addObject("maxPrice", maxPrice);
        modelAndView.addObject("availability", availability);

        return modelAndView;
    }

    @GetMapping("/new")
    public ModelAndView getCreateProductPage() {
        return createProductFormModelAndView(new ProductFormDto());
    }

    @GetMapping("/{id}/edit")
    public ModelAndView getEditProductPage(@PathVariable UUID id) {
        ProductFormDto productForm = productService.getProductForm(id);

        return createProductFormModelAndView(productForm);
    }

    @PostMapping
    public ModelAndView createProduct(
            @Valid @ModelAttribute("productForm") ProductFormDto productForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return createProductFormModelAndView(productForm);
        }

        try {
            productService.createProduct(productForm);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Product was created successfully."
            );

            return new ModelAndView("redirect:/admin/products");

        } catch (ProductNameAlreadyExistsException exception) {
            bindingResult.rejectValue(
                    "name",
                    "product.name.exists",
                    exception.getMessage()
            );

            return createProductFormModelAndView(productForm);
        }
    }

    @PutMapping("/{id}")
    public ModelAndView updateProduct(
            @PathVariable UUID id,
            @Valid @ModelAttribute("productForm") ProductFormDto productForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        productForm.setId(id);

        if (bindingResult.hasErrors()) {
            return createProductFormModelAndView(productForm);
        }

        try {
            productService.updateProduct(id, productForm);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Product was updated successfully."
            );

            return new ModelAndView("redirect:/admin/products");

        } catch (ProductNameAlreadyExistsException exception) {
            bindingResult.rejectValue(
                    "name",
                    "product.name.exists",
                    exception.getMessage()
            );

            return createProductFormModelAndView(productForm);
        }
    }

    @PutMapping("/{id}/availability")
    public ModelAndView toggleProductAvailability(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes
    ) {
        productService.toggleProductAvailability(id);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Product availability was updated."
        );

        return new ModelAndView("redirect:/admin/products");
    }

    @DeleteMapping("/{id}")
    public ModelAndView deleteProduct(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes
    ) {
        productService.deleteProduct(id);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Product was deleted successfully."
        );

        return new ModelAndView("redirect:/admin/products");
    }

    private ModelAndView createProductFormModelAndView(
            ProductFormDto productForm
    ) {
        ModelAndView modelAndView = new ModelAndView("admin/product-form");

        modelAndView.addObject("productForm", productForm);
        modelAndView.addObject("categories", ProductCategory.values());
        modelAndView.addObject("materials", PrintMaterial.values());

        return modelAndView;
    }

    private Boolean resolveAvailability(String availability) {
        if ("available".equals(availability)) {
            return true;
        }

        if ("hidden".equals(availability)) {
            return false;
        }

        return null;
    }
}