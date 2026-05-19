package com.example.demo.leavecore.delivery.Dto.keycloak;

import org.keycloak.representations.idm.GroupRepresentation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponse {
    String id;
    String name;
    String path;
    Map<String, String> attributes;
    List<GroupResponse> subGroups;

    public static GroupResponse from(GroupRepresentation groupRep) {
        if (groupRep == null) {
            return null;
        }

        Map<String, String> flattenedAttributes = Collections.emptyMap();
        if (groupRep.getAttributes() != null) {
            flattenedAttributes = groupRep.getAttributes().entrySet().stream()
                    .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().get(0)));
        }

        List<GroupResponse> childGroups = Collections.emptyList();
        if (groupRep.getSubGroups() != null) {
            childGroups = groupRep.getSubGroups().stream()
                    .map(GroupResponse::from)
                    .toList();
        }

        return new GroupResponse(
                groupRep.getId(),
                groupRep.getName(),
                groupRep.getPath(),
                flattenedAttributes,
                childGroups);
    }
}