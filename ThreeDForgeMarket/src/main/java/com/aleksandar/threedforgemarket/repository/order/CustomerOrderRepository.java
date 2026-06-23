package com.aleksandar.threedforgemarket.repository.order;

import com.aleksandar.threedforgemarket.model.entity.CustomerOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {
    @EntityGraph(attributePaths = "product")
    @Query("""
            SELECT customerOrder
            FROM CustomerOrder customerOrder
            WHERE customerOrder.customer.id = :customerId
              AND customerOrder.deletedFromCustomerHistory = false
            ORDER BY
                CASE
                    WHEN customerOrder.status = com.aleksandar.threedforgemarket.model.enums.order.OrderStatus.DELIVERED THEN 1
                    WHEN customerOrder.status = com.aleksandar.threedforgemarket.model.enums.order.OrderStatus.READY_FOR_DELIVERY THEN 2
                    WHEN customerOrder.status = com.aleksandar.threedforgemarket.model.enums.order.OrderStatus.PRINTING THEN 3
                    WHEN customerOrder.status = com.aleksandar.threedforgemarket.model.enums.order.OrderStatus.CONFIRMED THEN 4
                    WHEN customerOrder.status = com.aleksandar.threedforgemarket.model.enums.order.OrderStatus.PENDING THEN 5
                    WHEN customerOrder.status = com.aleksandar.threedforgemarket.model.enums.order.OrderStatus.CANCELLED THEN 6
                    ELSE 7
                END,
                customerOrder.createdOn DESC
            """)
    List<CustomerOrder> findVisibleForCustomerOrderedByStatus(
            @Param("customerId") UUID customerId
    );

    Optional<CustomerOrder> findByIdAndCustomer_Id(
            UUID orderId,
            UUID customerId
    );

    @EntityGraph(attributePaths = {"customer", "product"})
    @Query("""
            SELECT customerOrder
            FROM CustomerOrder customerOrder
            WHERE customerOrder.deletedFromAdminHistory = false
            ORDER BY
                CASE
                    WHEN customerOrder.status = com.aleksandar.threedforgemarket.model.enums.order.OrderStatus.DELIVERED THEN 1
                    WHEN customerOrder.status = com.aleksandar.threedforgemarket.model.enums.order.OrderStatus.READY_FOR_DELIVERY THEN 2
                    WHEN customerOrder.status = com.aleksandar.threedforgemarket.model.enums.order.OrderStatus.PRINTING THEN 3
                    WHEN customerOrder.status = com.aleksandar.threedforgemarket.model.enums.order.OrderStatus.CONFIRMED THEN 4
                    WHEN customerOrder.status = com.aleksandar.threedforgemarket.model.enums.order.OrderStatus.PENDING THEN 5
                    WHEN customerOrder.status = com.aleksandar.threedforgemarket.model.enums.order.OrderStatus.CANCELLED THEN 6
                    ELSE 7
                END,
                customerOrder.createdOn DESC
            """)
    List<CustomerOrder> findAllForAdminOrderedByStatus();

    boolean existsByProduct_Id(UUID productId);
}
