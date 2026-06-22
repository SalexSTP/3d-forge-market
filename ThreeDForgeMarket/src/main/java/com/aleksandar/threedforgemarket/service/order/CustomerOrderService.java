package com.aleksandar.threedforgemarket.service.order;

import com.aleksandar.threedforgemarket.exception.auth.UserNotFoundException;
import com.aleksandar.threedforgemarket.exception.order.CustomerOrderNotFoundException;
import com.aleksandar.threedforgemarket.exception.order.OrderCancellationNotAllowedException;
import com.aleksandar.threedforgemarket.exception.order.OrderCreationNotAllowedException;
import com.aleksandar.threedforgemarket.exception.order.ProductUnavailableException;
import com.aleksandar.threedforgemarket.exception.product.ProductNotFoundException;
import com.aleksandar.threedforgemarket.mapper.order.CustomerOrderMapper;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class CustomerOrderService {
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

        customerOrderRepository.save(customerOrder);
    }

    @Transactional(readOnly = true)
    public List<CustomerOrderListItemDto> getOrdersForCustomer(UUID customerId) {
        return customerOrderRepository
                .findAllByCustomer_IdOrderByCreatedOnDesc(customerId)
                .stream()
                .map(customerOrder -> customerOrderMapper.toListItemDto(
                        customerOrder,
                        isCancellable(customerOrder.getStatus())
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
    }

    private boolean isCancellable(OrderStatus status) {
        return status == OrderStatus.PENDING
                || status == OrderStatus.CONFIRMED;
    }
}