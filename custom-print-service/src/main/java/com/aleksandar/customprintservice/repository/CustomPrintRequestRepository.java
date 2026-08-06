package com.aleksandar.customprintservice.repository;

import com.aleksandar.customprintservice.model.entity.CustomPrintRequest;
import com.aleksandar.customprintservice.model.enums.CustomPrintRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomPrintRequestRepository extends JpaRepository<CustomPrintRequest, UUID> {

    List<CustomPrintRequest> findAllByCustomerIdOrderByCreatedOnDesc(UUID customerId);

    List<CustomPrintRequest> findAllByOrderByCreatedOnDesc();

    Optional<CustomPrintRequest> findByIdAndCustomerId(UUID id, UUID customerId);

    List<CustomPrintRequest> findAllByStatusOrderByCreatedOnDesc(CustomPrintRequestStatus status);
}
