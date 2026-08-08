package com.aleksandar.customprintservice.mapper;

import com.aleksandar.customprintservice.CustomPrintRequestTestData;
import com.aleksandar.customprintservice.model.dto.CustomPrintRequestDetailsDto;
import com.aleksandar.customprintservice.model.dto.CustomPrintRequestListItemDto;
import com.aleksandar.customprintservice.model.dto.UpdateCustomPrintRequestDto;
import com.aleksandar.customprintservice.model.entity.CustomPrintRequest;
import com.aleksandar.customprintservice.model.enums.CustomPrintRequestStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomPrintRequestMapperTest {

    private final CustomPrintRequestMapper mapper = new CustomPrintRequestMapper();

    @Test
    void toEntityMapsCreateDtoAndStartsPendingReview() {
        CustomPrintRequest request = mapper.toEntity(CustomPrintRequestTestData.validCreateDto());

        assertThat(request.getCustomerId()).isEqualTo(CustomPrintRequestTestData.CUSTOMER_ID);
        assertThat(request.getCustomerUsername()).isEqualTo("aleksandar");
        assertThat(request.getCustomerEmail()).isEqualTo("aleksandar@example.com");
        assertThat(request.getTitle()).isEqualTo("Custom phone stand");
        assertThat(request.getStatus()).isEqualTo(CustomPrintRequestStatus.PENDING_REVIEW);
        assertThat(request.isAdminAttentionRequired()).isFalse();
        assertThat(request.isCustomerResponseReminderRequired()).isFalse();
    }

    @Test
    void updateEntityReplacesEditableCustomerFields() {
        CustomPrintRequest request = CustomPrintRequestTestData.validEntity(CustomPrintRequestStatus.PENDING_REVIEW);
        UpdateCustomPrintRequestDto updateDto = CustomPrintRequestTestData.validUpdateDto();

        mapper.updateEntity(request, updateDto);

        assertThat(request.getTitle()).isEqualTo(updateDto.title());
        assertThat(request.getDescription()).isEqualTo(updateDto.description());
        assertThat(request.getMaterial()).isEqualTo(updateDto.material());
        assertThat(request.getColorDescription()).isEqualTo(updateDto.colorDescription());
        assertThat(request.getDeliveryAddress()).isEqualTo(updateDto.deliveryAddress());
        assertThat(request.getWidthCm()).isEqualByComparingTo(updateDto.widthCm());
        assertThat(request.getHeightCm()).isEqualByComparingTo(updateDto.heightCm());
        assertThat(request.getDepthCm()).isEqualByComparingTo(updateDto.depthCm());
        assertThat(request.getQuantity()).isEqualTo(updateDto.quantity());
        assertThat(request.getReferenceFileUrl()).isEqualTo(updateDto.referenceFileUrl());
    }

    @Test
    void toListItemDtoMapsMaintenanceFields() {
        CustomPrintRequest request = CustomPrintRequestTestData.mappedEntityWithMaintenanceFields();

        CustomPrintRequestListItemDto dto = mapper.toListItemDto(request);

        assertThat(dto.id()).isEqualTo(request.getId());
        assertThat(dto.status()).isEqualTo(CustomPrintRequestStatus.OFFER_SENT);
        assertThat(dto.adminAttentionRequired()).isTrue();
        assertThat(dto.adminAttentionMarkedOn()).isEqualTo(request.getAdminAttentionMarkedOn());
        assertThat(dto.customerResponseReminderRequired()).isTrue();
        assertThat(dto.customerResponseReminderMarkedOn()).isEqualTo(request.getCustomerResponseReminderMarkedOn());
    }

    @Test
    void toDetailsDtoMapsMaintenanceFieldsAndAutoArchiveDate() {
        CustomPrintRequest request = CustomPrintRequestTestData.mappedEntityWithMaintenanceFields();

        CustomPrintRequestDetailsDto dto = mapper.toDetailsDto(request);

        assertThat(dto.id()).isEqualTo(request.getId());
        assertThat(dto.description()).isEqualTo(request.getDescription());
        assertThat(dto.adminAttentionRequired()).isTrue();
        assertThat(dto.adminAttentionMarkedOn()).isEqualTo(request.getAdminAttentionMarkedOn());
        assertThat(dto.customerResponseReminderRequired()).isTrue();
        assertThat(dto.customerResponseReminderMarkedOn()).isEqualTo(request.getCustomerResponseReminderMarkedOn());
        assertThat(dto.autoArchivedOn()).isEqualTo(request.getAutoArchivedOn());
    }
}
