package com.aleksandar.threedforgemarket.web.controller.order;

import com.aleksandar.threedforgemarket.exception.order.CustomerOrderNotFoundException;
import com.aleksandar.threedforgemarket.exception.order.OrderCancellationNotAllowedException;
import com.aleksandar.threedforgemarket.exception.order.OrderCreationNotAllowedException;
import com.aleksandar.threedforgemarket.exception.order.OrderDeletionNotAllowedException;
import com.aleksandar.threedforgemarket.exception.order.ProductUnavailableException;
import com.aleksandar.threedforgemarket.exception.product.ProductNotFoundException;
import com.aleksandar.threedforgemarket.model.dto.order.CreateOrderRequest;
import com.aleksandar.threedforgemarket.model.dto.product.ProductDetailsDto;
import com.aleksandar.threedforgemarket.service.order.CustomerOrderService;
import com.aleksandar.threedforgemarket.service.product.ProductService;
import com.aleksandar.threedforgemarket.web.controller.auth.AuthController;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.UUID;

@Controller
@RequestMapping("/orders")
public class CustomerOrderController {

    private final CustomerOrderService customerOrderService;
    private final ProductService productService;

    public CustomerOrderController(
            CustomerOrderService customerOrderService,
            ProductService productService
    ) {
        this.customerOrderService = customerOrderService;
        this.productService = productService;
    }

    @GetMapping("/create")
    public ModelAndView getCreateOrderPage(
            @RequestParam UUID productId,
            RedirectAttributes redirectAttributes
    ) {
        CreateOrderRequest orderForm = new CreateOrderRequest();
        orderForm.setProductId(productId);
        orderForm.setQuantity(1);

        try {
            return createOrderFormModelAndView(orderForm);

        } catch (ProductNotFoundException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This product is no longer available for ordering."
            );

            return new ModelAndView("redirect:/products");
        }
    }

    @PostMapping("/create")
    public ModelAndView createOrder(
            @Valid @ModelAttribute("orderForm") CreateOrderRequest orderForm,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasFieldErrors("productId")) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please choose a product before placing an order."
            );

            return new ModelAndView("redirect:/products");
        }

        if (bindingResult.hasErrors()) {
            try {
                return createOrderFormModelAndView(orderForm);

            } catch (ProductNotFoundException exception) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "This product is no longer available for ordering."
                );

                return new ModelAndView("redirect:/products");
            }
        }

        try {
            customerOrderService.createOrder(
                    getCurrentUserId(session),
                    orderForm
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Your order was placed successfully."
            );

            return new ModelAndView("redirect:/orders/my");

        } catch (ProductUnavailableException | ProductNotFoundException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This product is no longer available for ordering."
            );

            return new ModelAndView("redirect:/products");

        } catch (OrderCreationNotAllowedException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

            return new ModelAndView("redirect:/");
        }
    }

    @GetMapping("/my")
    public ModelAndView getMyOrdersPage(HttpSession session) {
        ModelAndView modelAndView = new ModelAndView("order/my-orders");

        modelAndView.addObject(
                "orders",
                customerOrderService.getOrdersForCustomer(
                        getCurrentUserId(session)
                )
        );

        return modelAndView;
    }

    @PutMapping("/{id}/cancel")
    public ModelAndView cancelOrder(
            @PathVariable UUID id,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        try {
            customerOrderService.cancelOrder(
                    getCurrentUserId(session),
                    id
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Your order was cancelled successfully."
            );

        } catch (CustomerOrderNotFoundException
                 | OrderCancellationNotAllowedException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return new ModelAndView("redirect:/orders/my");
    }

    @DeleteMapping("/{id}")
    public ModelAndView deleteOrderFromHistory(
            @PathVariable UUID id,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        try {
            customerOrderService.deleteOrderFromHistory(
                    getCurrentUserId(session),
                    id
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "The cancelled order was removed from your history."
            );

        } catch (CustomerOrderNotFoundException
                 | OrderDeletionNotAllowedException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return new ModelAndView("redirect:/orders/my");
    }

    private ModelAndView createOrderFormModelAndView(
            CreateOrderRequest orderForm
    ) {
        ProductDetailsDto product = productService.getAvailableProductDetails(
                orderForm.getProductId()
        );

        int quantity = orderForm.getQuantity() != null && orderForm.getQuantity() >= 1
                ? orderForm.getQuantity()
                : 1;

        BigDecimal calculatedTotal = product.getPrice()
                .multiply(BigDecimal.valueOf(quantity));

        ModelAndView modelAndView = new ModelAndView("order/create");

        modelAndView.addObject("product", product);
        modelAndView.addObject("orderForm", orderForm);
        modelAndView.addObject("calculatedTotal", calculatedTotal);

        return modelAndView;
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
