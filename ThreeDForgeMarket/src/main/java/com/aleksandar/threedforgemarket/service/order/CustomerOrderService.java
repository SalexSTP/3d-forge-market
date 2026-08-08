package com.aleksandar.threedforgemarket.service.order;

import com.aleksandar.threedforgemarket.exception.auth.UserNotFoundException;
import com.aleksandar.threedforgemarket.exception.order.CustomerOrderNotFoundException;
import com.aleksandar.threedforgemarket.exception.order.OrderCancellationNotAllowedException;
import com.aleksandar.threedforgemarket.exception.order.OrderCreationNotAllowedException;
import com.aleksandar.threedforgemarket.exception.order.OrderDeletionNotAllowedException;
import com.aleksandar.threedforgemarket.exception.order.OrderStatusUpdateNotAllowedException;
import com.aleksandar.threedforgemarket.exception.order.ProductUnavailableException;
import com.aleksandar.threedforgemarket.exception.product.ProductNotFoundException;
import com.aleksandar.threedforgemarket.mapper.order.CustomerOrderMapper;
import com.aleksandar.threedforgemarket.model.dto.order.AdminOrderListItemDto;
import com.aleksandar.threedforgemarket.model.dto.order.CreateOrderRequest;
import com.aleksandar.threedforgemarket.model.dto.order.CustomerOrderListItemDto;
import com.aleksandar.threedforgemarket.model.entity.CustomerOrder;
import com.aleksandar.threedforgemarket.model.entity.Product;
import com.aleksandar.threedforgemarket.model.entity.User;
import com.aleksandar.threedforgemarket.model.enums.order.OrderStatus;
import com.aleksandar.threedforgemarket.model.enums.user.UserRole;
import com.aleksandar.threedforgemarket.repository.order.CustomerOrderRepository;
import com.aleksandar.threedforgemarket.repository.product.ProductRepository;
import com.aleksandar.threedforgemarket.repository.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class CustomerOrderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerOrderService.class);

    private final CustomerOrderRepository customerOrderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CustomerOrderMapper customerOrderMapper;

    public CustomerOrderService(
            CustomerOrderRepository customerOrderRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            CustomerOrderMapper customerOrderMapper
    ) {
        this.customerOrderRepository = customerOrderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.customerOrderMapper = customerOrderMapper;
    }

    @Transactional
    public void createOrder(
            UUID customerId,
            CreateOrderRequest orderRequest
    ) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(UserNotFoundException::new);

        if (customer.getRole() != UserRole.CUSTOMER) {
            throw new OrderCreationNotAllowedException();
        }

        Product product = productRepository.findById(orderRequest.getProductId())
                .orElseThrow(ProductNotFoundException::new);

        if (!product.isAvailable()) {
            throw new ProductUnavailableException();
        }

        BigDecimal totalPrice = product.getPrice()
                .multiply(BigDecimal.valueOf(orderRequest.getQuantity()));

        CustomerOrder customerOrder = customerOrderMapper.toEntity(
                orderRequest,
                product,
                customer,
                totalPrice
        );

        CustomerOrder savedOrder = customerOrderRepository.save(customerOrder);
        LOGGER.info("Created order with id={} for customer id={} and product id={}",
                savedOrder.getId(),
                customer.getId(),
                product.getId());
    }

    @Transactional(readOnly = true)
    public List<CustomerOrderListItemDto> getOrdersForCustomer(UUID customerId) {
        return customerOrderRepository
                .findVisibleForCustomerOrderedByStatus(customerId)
                .stream()
                .map(customerOrder -> customerOrderMapper.toListItemDto(
                        customerOrder,
                        isCancellable(customerOrder.getStatus()),
                        isDeletable(customerOrder.getStatus())
                ))
                .toList();
    }

    @Transactional
    public void cancelOrder(UUID customerId, UUID orderId) {
        CustomerOrder customerOrder = customerOrderRepository
                .findByIdAndCustomer_Id(orderId, customerId)
                .orElseThrow(CustomerOrderNotFoundException::new);

        if (!isCancellable(customerOrder.getStatus())) {
            throw new OrderCancellationNotAllowedException();
        }

        customerOrder.setStatus(OrderStatus.CANCELLED);

        customerOrderRepository.save(customerOrder);
        LOGGER.info("Cancelled order id={} for customer id={}", orderId, customerId);
    }

    @Transactional
    public void deleteOrderFromHistory(UUID customerId, UUID orderId) {
        CustomerOrder customerOrder = customerOrderRepository
                .findByIdAndCustomer_Id(orderId, customerId)
                .orElseThrow(CustomerOrderNotFoundException::new);

        if (!isDeletable(customerOrder.getStatus())) {
            throw new OrderDeletionNotAllowedException();
        }

        customerOrder.setDeletedFromCustomerHistory(true);

        customerOrderRepository.save(customerOrder);
        LOGGER.info("Removed order id={} from customer history for customer id={}", orderId, customerId);
    }

    @Transactional(readOnly = true)
    public List<AdminOrderListItemDto> getAllOrdersForAdmin() {
        return customerOrderRepository.findAllForAdminOrderedByStatus()
                .stream()
                .map(customerOrder -> customerOrderMapper.toAdminListItemDto(
                        customerOrder,
                        getAvailableStatusUpdates(customerOrder.getStatus()),
                        isDeletable(customerOrder.getStatus())
                ))
                .toList();
    }

    @Transactional
    public void updateOrderStatus(
            UUID orderId,
            OrderStatus requestedStatus
    ) {
        CustomerOrder customerOrder = customerOrderRepository.findById(orderId)
                .orElseThrow(CustomerOrderNotFoundException::new);

        if (!getAvailableStatusUpdates(customerOrder.getStatus())
                .contains(requestedStatus)) {
            throw new OrderStatusUpdateNotAllowedException();
        }

        customerOrder.setStatus(requestedStatus);

        customerOrderRepository.save(customerOrder);
        LOGGER.info("Updated order status to {} for order id={}", requestedStatus, orderId);
    }

    @Transactional
    public void deleteOrderFromAdminHistory(UUID orderId) {
        CustomerOrder customerOrder = customerOrderRepository.findById(orderId)
                .orElseThrow(CustomerOrderNotFoundException::new);

        if (!isDeletable(customerOrder.getStatus())) {
            throw new OrderDeletionNotAllowedException();
        }

        customerOrder.setDeletedFromAdminHistory(true);

        customerOrderRepository.save(customerOrder);
        LOGGER.info("Removed order id={} from admin history", orderId);
    }

    private boolean isCancellable(OrderStatus status) {
        return status == OrderStatus.PENDING
                || status == OrderStatus.CONFIRMED;
    }

    private boolean isDeletable(OrderStatus status) {
        return status == OrderStatus.DELIVERED
                || status == OrderStatus.CANCELLED;
    }

    private List<OrderStatus> getAvailableStatusUpdates(
            OrderStatus currentStatus
    ) {
        return switch (currentStatus) {
            case PENDING -> List.of(
                    OrderStatus.CONFIRMED,
                    OrderStatus.CANCELLED
            );

            case CONFIRMED -> List.of(
                    OrderStatus.PRINTING,
                    OrderStatus.CANCELLED
            );

            case PRINTING -> List.of(
                    OrderStatus.READY_FOR_DELIVERY
            );

            case READY_FOR_DELIVERY -> List.of(
                    OrderStatus.DELIVERED
            );

            case DELIVERED, CANCELLED -> List.of();
        };
    }
}
