package com.aleksandar.threedforgemarket.repository.order;

import com.aleksandar.threedforgemarket.model.entity.CustomerOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {
    @EntityGraph(attributePaths = "product")
    List<CustomerOrder> findAllByCustomer_IdOrderByCreatedOnDesc(UUID customerId);

    Optional<CustomerOrder> findByIdAndCustomer_Id(
            UUID orderId,
            UUID customerId
    );
}
