package com.example.demo.leavecore.delivery.Mapper;

import com.example.demo.leavecore.delivery.Dto.Holiday.HolidayRequest.HolidayCreateRequest;
import com.example.demo.leavecore.delivery.Dto.Holiday.HolidayRequest.HolidayUpdateRequest;
import com.example.demo.leavecore.delivery.Dto.Holiday.HolidayResponse;
import com.example.demo.leavecore.domain.entity.Holiday;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface HolidayMapper {

    @Mapping(target = "id", ignore = true)
    Holiday toEntity(HolidayCreateRequest request);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(HolidayUpdateRequest request, @MappingTarget Holiday entity);

    HolidayResponse toResponse(Holiday entity);

}
