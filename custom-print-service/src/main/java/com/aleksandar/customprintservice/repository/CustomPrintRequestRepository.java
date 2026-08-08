package com.aleksandar.customprintservice.repository;

import com.aleksandar.customprintservice.model.entity.CustomPrintRequest;
import com.aleksandar.customprintservice.model.enums.CustomPrintRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomPrintRequestRepository extends JpaRepository<CustomPrintRequest, UUID> {

    Optional<CustomPrintRequest> findByIdAndCustomerId(UUID id, UUID customerId);

    @Query("""
            select r
            from CustomPrintRequest r
            where r.customerId = :customerId
              and r.hiddenFromCustomer = false
              and (:status is null or r.status = :status)
              and (:createdFrom is null or r.createdOn >= :createdFrom)
              and (:createdTo is null or r.createdOn <= :createdTo)
              and (
                    :keyword is null
                    or lower(r.title) like lower(concat('%', :keyword, '%'))
                    or lower(r.material) like lower(concat('%', :keyword, '%'))
                    or lower(r.colorDescription) like lower(concat('%', :keyword, '%'))
                    or lower(r.description) like lower(concat('%', :keyword, '%'))
              )
            order by r.createdOn desc
            """)
    List<CustomPrintRequest> searchCustomerRequests(
            @Param("customerId") UUID customerId,
            @Param("keyword") String keyword,
            @Param("status") CustomPrintRequestStatus status,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo);

    @Query("""
            select r
            from CustomPrintRequest r
            where r.hiddenFromAdmin = false
              and (:status is null or r.status = :status)
              and (:createdFrom is null or r.createdOn >= :createdFrom)
              and (:createdTo is null or r.createdOn <= :createdTo)
              and (
                    :keyword is null
                    or lower(r.title) like lower(concat('%', :keyword, '%'))
                    or lower(r.material) like lower(concat('%', :keyword, '%'))
                    or lower(r.colorDescription) like lower(concat('%', :keyword, '%'))
                    or lower(r.description) like lower(concat('%', :keyword, '%'))
                    or lower(r.customerUsername) like lower(concat('%', :keyword, '%'))
                    or lower(r.customerEmail) like lower(concat('%', :keyword, '%'))
              )
            order by r.createdOn desc
            """)
    List<CustomPrintRequest> searchAdminRequests(
            @Param("keyword") String keyword,
            @Param("status") CustomPrintRequestStatus status,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo);

    @Query("""
            select r
            from CustomPrintRequest r
            where r.status in :statuses
              and r.updatedOn <= :staleBefore
              and r.adminAttentionRequired = false
            """)
    List<CustomPrintRequest> findRequestsWaitingForAdminAttention(
            @Param("statuses") List<CustomPrintRequestStatus> statuses,
            @Param("staleBefore") LocalDateTime staleBefore);

    @Query("""
            select r
            from CustomPrintRequest r
            where r.status = :status
              and r.quotedOn <= :staleBefore
              and r.customerResponseReminderRequired = false
            """)
    List<CustomPrintRequest> findOffersWaitingForCustomerResponse(
            @Param("status") CustomPrintRequestStatus status,
            @Param("staleBefore") LocalDateTime staleBefore);

    @Query("""
            select r
            from CustomPrintRequest r
            where r.status in :statuses
              and r.updatedOn <= :staleBefore
              and r.hiddenFromAdmin = false
            """)
    List<CustomPrintRequest> findOldFinishedRequestsVisibleToAdmin(
            @Param("statuses") List<CustomPrintRequestStatus> statuses,
            @Param("staleBefore") LocalDateTime staleBefore);
}
