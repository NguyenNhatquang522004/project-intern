package com.example.demo.common.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

@Provider
public class KeycloakJacksonConfig implements ContextResolver<ObjectMapper> {
    private final ObjectMapper objectMapper;

    public KeycloakJacksonConfig() {
        this.objectMapper = new ObjectMapper();
        // Ép cấu hình bỏ qua thuộc tính ẩn danh/không nhận diện được
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return objectMapper;
    }
}
