package com.aleksandar.threedforgemarket.web.controller.order;

import com.aleksandar.threedforgemarket.exception.order.CustomerOrderNotFoundException;
import com.aleksandar.threedforgemarket.exception.order.OrderDeletionNotAllowedException;
import com.aleksandar.threedforgemarket.exception.order.OrderStatusUpdateNotAllowedException;
import com.aleksandar.threedforgemarket.model.enums.order.OrderStatus;
import com.aleksandar.threedforgemarket.service.order.CustomerOrderService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final CustomerOrderService customerOrderService;

    public AdminOrderController(CustomerOrderService customerOrderService) {
        this.customerOrderService = customerOrderService;
    }

    @GetMapping
    public ModelAndView getAdminOrdersPage() {
        ModelAndView modelAndView = new ModelAndView("admin/orders");

        modelAndView.addObject(
                "orders",
                customerOrderService.getAllOrdersForAdmin()
        );

        return modelAndView;
    }

    @PutMapping("/{id}/status")
    public ModelAndView updateOrderStatus(
            @PathVariable UUID id,
            @RequestParam OrderStatus status,
            RedirectAttributes redirectAttributes
    ) {
        try {
            customerOrderService.updateOrderStatus(id, status);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Order status was updated successfully."
            );

        } catch (CustomerOrderNotFoundException
                 | OrderStatusUpdateNotAllowedException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return new ModelAndView("redirect:/admin/orders");
    }

    @DeleteMapping("/{id}")
    public ModelAndView deleteOrderFromAdminHistory(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            customerOrderService.deleteOrderFromAdminHistory(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "The order was removed from the admin history."
            );

        } catch (CustomerOrderNotFoundException
                 | OrderDeletionNotAllowedException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );
        }

        return new ModelAndView("redirect:/admin/orders");
    }
}
