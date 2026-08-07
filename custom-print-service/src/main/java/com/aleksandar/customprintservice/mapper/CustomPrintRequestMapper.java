package com.aleksandar.customprintservice.mapper;

import com.aleksandar.customprintservice.model.dto.CreateCustomPrintRequestDto;
import com.aleksandar.customprintservice.model.dto.CustomPrintRequestDetailsDto;
import com.aleksandar.customprintservice.model.dto.CustomPrintRequestListItemDto;
import com.aleksandar.customprintservice.model.entity.CustomPrintRequest;
import com.aleksandar.customprintservice.model.enums.CustomPrintRequestStatus;
import org.springframework.stereotype.Component;

@Component
public class CustomPrintRequestMapper {

    public CustomPrintRequest toEntity(CreateCustomPrintRequestDto requestDto) {
        CustomPrintRequest request = new CustomPrintRequest();
        request.setCustomerId(requestDto.customerId());
        request.setCustomerUsername(requestDto.customerUsername());
        request.setCustomerEmail(requestDto.customerEmail());
        request.setTitle(requestDto.title());
        request.setDescription(requestDto.description());
        request.setMaterial(requestDto.material());
        request.setColorDescription(requestDto.colorDescription());
        request.setDeliveryAddress(requestDto.deliveryAddress());
        request.setWidthCm(requestDto.widthCm());
        request.setHeightCm(requestDto.heightCm());
        request.setDepthCm(requestDto.depthCm());
        request.setQuantity(requestDto.quantity());
        request.setReferenceFileUrl(requestDto.referenceFileUrl());
        request.setStatus(CustomPrintRequestStatus.PENDING_REVIEW);

        return request;
    }

    public CustomPrintRequestListItemDto toListItemDto(CustomPrintRequest request) {
        return new CustomPrintRequestListItemDto(
                request.getId(),
                request.getCustomerId(),
                request.getCustomerUsername(),
                request.getCustomerEmail(),
                request.getTitle(),
                request.getMaterial(),
                request.getColorDescription(),
                request.getDeliveryAddress(),
                request.getWidthCm(),
                request.getHeightCm(),
                request.getDepthCm(),
                request.getQuantity(),
                request.getReferenceFileUrl(),
                request.getResponseFileUrl(),
                request.getStatus(),
                request.getQuotedPrice(),
                request.getEstimatedPrintTimeMinutes(),
                request.getAdminMessage(),
                request.getCustomerMessage(),
                request.getCreatedOn(),
                request.getUpdatedOn(),
                request.getQuotedOn(),
                request.getCustomerRespondedOn(),
                request.getAcceptedOn(),
                request.getPrintingStartedOn(),
                request.getReadyForDeliveryOn(),
                request.getDeliveredOn(),
                request.getCancelledOn()
        );
    }

    public CustomPrintRequestDetailsDto toDetailsDto(CustomPrintRequest request) {
        return new CustomPrintRequestDetailsDto(
                request.getId(),
                request.getCustomerId(),
                request.getCustomerUsername(),
                request.getCustomerEmail(),
                request.getTitle(),
                request.getDescription(),
                request.getMaterial(),
                request.getColorDescription(),
                request.getDeliveryAddress(),
                request.getWidthCm(),
                request.getHeightCm(),
                request.getDepthCm(),
                request.getQuantity(),
                request.getReferenceFileUrl(),
                request.getResponseFileUrl(),
                request.getStatus(),
                request.getQuotedPrice(),
                request.getEstimatedPrintTimeMinutes(),
                request.getAdminMessage(),
                request.getCustomerMessage(),
                request.getCreatedOn(),
                request.getUpdatedOn(),
                request.getQuotedOn(),
                request.getCustomerRespondedOn(),
                request.getAcceptedOn(),
                request.getPrintingStartedOn(),
                request.getReadyForDeliveryOn(),
                request.getDeliveredOn(),
                request.getCancelledOn()
        );
    }
}
