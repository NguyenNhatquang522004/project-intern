package com.example.demo.leavecore.delivery.Dto.tasklist;

import java.util.Map;

public record TaskItemDto(
        String id,
        String name,
        String processId,
        String processInstanceKey,
        String processDefinitionKey,
        String creationTime,
        String assignee,
        String taskState,
        String formId,
        Map<String, Object> variables
) {}
