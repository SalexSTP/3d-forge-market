package com.aleksandar.threedforgemarket.web.controller.order;

import com.aleksandar.threedforgemarket.exception.order.CustomerOrderNotFoundException;
import com.aleksandar.threedforgemarket.exception.order.OrderCancellationNotAllowedException;
import com.aleksandar.threedforgemarket.exception.order.OrderCreationNotAllowedException;
import com.aleksandar.threedforgemarket.exception.order.OrderDeletionNotAllowedException;
import com.aleksandar.threedforgemarket.exception.order.ProductUnavailableException;
import com.aleksandar.threedforgemarket.exception.payment.PaymentOperationFailedException;
import com.aleksandar.threedforgemarket.exception.payment.StripePaymentUnavailableException;
import com.aleksandar.threedforgemarket.exception.product.ProductNotFoundException;
import com.aleksandar.threedforgemarket.model.dto.order.CreatedOrderDto;
import com.aleksandar.threedforgemarket.model.dto.order.CreateOrderRequest;
import com.aleksandar.threedforgemarket.model.dto.payment.PaymentStartResult;
import com.aleksandar.threedforgemarket.model.enums.payment.PaymentMethod;
import com.aleksandar.threedforgemarket.model.dto.product.ProductDetailsDto;
import com.aleksandar.threedforgemarket.security.MarketplaceUserDetails;
import com.aleksandar.threedforgemarket.service.order.CustomerOrderService;
import com.aleksandar.threedforgemarket.service.payment.PaymentService;
import com.aleksandar.threedforgemarket.service.product.ProductService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
    private final PaymentService paymentService;

    public CustomerOrderController(
            CustomerOrderService customerOrderService,
            ProductService productService,
            PaymentService paymentService
    ) {
        this.customerOrderService = customerOrderService;
        this.productService = productService;
        this.paymentService = paymentService;
    }

    @GetMapping("/create")
    public ModelAndView getCreateOrderPage(
            @RequestParam UUID productId,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        CreateOrderRequest orderForm;

        if (model.containsAttribute("orderForm")) {
            orderForm = (CreateOrderRequest) model.asMap().get("orderForm");
            orderForm.setProductId(productId);
        } else {
            orderForm = new CreateOrderRequest();
            orderForm.setProductId(productId);
            orderForm.setQuantity(1);
            orderForm.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        }

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
            @AuthenticationPrincipal MarketplaceUserDetails currentUser,
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

        if (orderForm.getPaymentMethod() == PaymentMethod.STRIPE_CHECKOUT
                && !paymentService.isStripeCheckoutAvailable()) {
            bindingResult.rejectValue(
                    "paymentMethod",
                    "paymentMethod.stripeUnavailable",
                    "Online payments are currently unavailable."
            );

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
            CreatedOrderDto order = customerOrderService.createOrder(
                    currentUser.getId(),
                    orderForm
            );

            PaymentStartResult paymentStartResult = paymentService.startProductOrderPayment(
                    order,
                    orderForm.getPaymentMethod()
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    orderForm.getPaymentMethod() == PaymentMethod.STRIPE_CHECKOUT
                            ? "Your order was placed. Complete payment in Stripe Checkout."
                            : "Your order was placed successfully."
            );

            return new ModelAndView("redirect:" + paymentStartResult.redirectUrl());

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
        } catch (StripePaymentUnavailableException | PaymentOperationFailedException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return new ModelAndView("redirect:/orders/my");
        }
    }

    @GetMapping({"", "/my"})
    public ModelAndView getMyOrdersPage(
            @AuthenticationPrincipal MarketplaceUserDetails currentUser
    ) {
        ModelAndView modelAndView = new ModelAndView("order/my-orders");

        modelAndView.addObject(
                "orders",
                customerOrderService.getOrdersForCustomer(
                        currentUser.getId()
                )
        );

        return modelAndView;
    }

    @PutMapping("/{id}/cancel")
    public ModelAndView cancelOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal MarketplaceUserDetails currentUser,
            RedirectAttributes redirectAttributes
    ) {
        try {
            customerOrderService.cancelOrder(
                    currentUser.getId(),
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
            @AuthenticationPrincipal MarketplaceUserDetails currentUser,
            RedirectAttributes redirectAttributes
    ) {
        try {
            customerOrderService.deleteOrderFromHistory(
                    currentUser.getId(),
                    id
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "The order was removed from your history."
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
        modelAndView.addObject("paymentMethods", PaymentMethod.values());
        modelAndView.addObject("stripeAvailable", paymentService.isStripeCheckoutAvailable());

        return modelAndView;
    }
}
