package com.aleksandar.customprintservice.model.entity;

import com.aleksandar.customprintservice.model.enums.CustomPrintRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "custom_print_requests")
public class CustomPrintRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false, length = 30)
    private String customerUsername;

    @Column(nullable = false, length = 100)
    private String customerEmail;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false, length = 50)
    private String material;

    @Column(nullable = false, length = 80)
    private String colorDescription;

    @Column(nullable = false, length = 250)
    private String deliveryAddress;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal widthCm;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal heightCm;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal depthCm;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 500)
    private String referenceFileUrl;

    @Column(length = 500)
    private String responseFileUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CustomPrintRequestStatus status;

    @Column(precision = 10, scale = 2)
    private BigDecimal quotedPrice;

    private Integer estimatedPrintTimeMinutes;

    @Column(length = 1000)
    private String adminMessage;

    @Column(length = 1000)
    private String customerMessage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(nullable = false)
    private LocalDateTime updatedOn;

    private LocalDateTime quotedOn;

    private LocalDateTime customerRespondedOn;

    private LocalDateTime acceptedOn;

    private LocalDateTime printingStartedOn;

    private LocalDateTime readyForDeliveryOn;

    private LocalDateTime deliveredOn;

    private LocalDateTime cancelledOn;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean hiddenFromCustomer = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean hiddenFromAdmin = false;

    private LocalDateTime hiddenFromCustomerOn;

    private LocalDateTime hiddenFromAdminOn;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdOn = now;
        this.updatedOn = now;

        if (this.status == null) {
            this.status = CustomPrintRequestStatus.PENDING_REVIEW;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedOn = LocalDateTime.now();
    }
}
