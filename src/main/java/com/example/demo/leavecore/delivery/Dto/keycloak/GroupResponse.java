package com.example.demo.leavecore.delivery.Dto.keycloak;

import org.keycloak.representations.idm.GroupRepresentation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public record GroupResponse(
        String id,
        String name,
        String path,
        Map<String, String> attributes, 
        List<GroupResponse> subGroups
) {
    public static GroupResponse from(GroupRepresentation groupRep) {
        if (groupRep == null) {
            return null;
        }

        // 1. Phẳng hóa Custom Attributes của Group (Tương tự như User)
        Map<String, String> flattenedAttributes = Collections.emptyMap();
        if (groupRep.getAttributes() != null) {
            flattenedAttributes = groupRep.getAttributes().entrySet().stream()
                    .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().get(0) // Lấy giá trị cấu hình đầu tiên
                    ));
        }

        // 2. Xử lý Đệ quy (Recursion) danh sách Group con dữ liệu chuẩn xác
        List<GroupResponse> childGroups = Collections.emptyList();
        if (groupRep.getSubGroups() != null) {
            childGroups = groupRep.getSubGroups().stream()
                    .map(GroupResponse::from) // Tự động gọi lại chính nó để map các tầng sâu hơn
                    .toList();
        }

        return new GroupResponse(
                groupRep.getId(),
                groupRep.getName(),
                groupRep.getPath(),
                flattenedAttributes,
                childGroups
        );
    }
}