package com.example.demo.leavecore.delivery.Dto.keycloak;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.keycloak.representations.idm.UserRepresentation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
        String id;
        String username;
        String email;
        String firstName;
        String lastName;
        boolean enabled;
        boolean emailVerified;
        Instant createdAt;
        Map<String, String> attributes;

        public static UserResponse from(UserRepresentation userRep) {
                if (userRep == null) {
                        return null;
                }

                // 1. Xử lý thời gian: Keycloak trả về Long (Epoch Milliseconds), Convert sang
                // Instant chuẩn ISO-8601
                Instant createdAtInstant = userRep.getCreatedTimestamp() != null
                                ? Instant.ofEpochMilli(userRep.getCreatedTimestamp())
                                : null;

                // 2. Phẳng hóa Custom Attributes: Keycloak lưu Map<String, List<String>>.
                // Thực tế 99% các trường custom (SĐT, Địa chỉ...) chỉ cần 1 giá trị đầu tiên.
                Map<String, String> flattenedAttributes = Collections.emptyMap();
                if (userRep.getAttributes() != null) {
                        flattenedAttributes = userRep.getAttributes().entrySet().stream()
                                        .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                                        .collect(Collectors.toMap(
                                                        Map.Entry::getKey,
                                                        entry -> entry.getValue().get(0) // Lấy phần tử đầu tiên của
                                                                                         // List
                                        ));
                }

                return new UserResponse(
                                userRep.getId(),
                                userRep.getUsername(),
                                userRep.getEmail(),
                                userRep.getFirstName(),
                                userRep.getLastName(),
                                userRep.isEnabled(),
                                userRep.isEmailVerified(),
                                createdAtInstant,
                                flattenedAttributes);
        }

}