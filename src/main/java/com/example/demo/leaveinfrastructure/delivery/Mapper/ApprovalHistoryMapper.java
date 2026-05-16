package com.example.demo.leaveinfrastructure.delivery.Mapper;

import com.example.demo.leaveinfrastructure.delivery.Dto.ApprovalHistory.ApprovalHistoryRequest.ApprovalHistoryCreateRequest;
import com.example.demo.leaveinfrastructure.delivery.Dto.ApprovalHistory.ApprovalHistoryRequest.ApprovalHistoryUpdateRequest;
import com.example.demo.leaveinfrastructure.delivery.Dto.ApprovalHistory.ApprovalHistoryResponse;
import com.example.demo.leaveinfrastructure.domain.entity.ApprovalHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ApprovalHistoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    ApprovalHistory toEntity(ApprovalHistoryCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromRequest(ApprovalHistoryUpdateRequest request, @MappingTarget ApprovalHistory entity);

    ApprovalHistoryResponse toResponse(ApprovalHistory entity);
}
