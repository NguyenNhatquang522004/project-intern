package com.example.demo.integration.config;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProvider;
import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProviderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * TestConfiguration: Cấu hình ZeebeClient kết nối thực tế đến cluster local
 * (Zeebe gRPC tại localhost:26500, bảo mật OAuth2 qua Keycloak).
 */
@TestConfiguration
public class ZeebeTestConfig {

    @Value("${camunda.client.auth.issuer-url:http://localhost:18080/auth/realms/camunda-platform}")
    private String issuerUrl;

    @Value("${camunda.client.auth.client-id:orchestration}")
    private String clientId;

    @Value("${camunda.client.auth.client-secret:secret}")
    private String clientSecret;

    @Value("${camunda.client.zeebe.gateway-address:localhost:26500}")
    private String gatewayAddress;

    /**
     * Bean ZeebeClient primary dùng trong tất cả integration tests.
     * Kết nối plaintext (không TLS) đến Zeebe local.
     * Token OAuth2 được lấy từ Keycloak.
     */
    @Bean
    @Primary
    public ZeebeClient zeebeTestClient() {
        OAuthCredentialsProvider credentialsProvider = new OAuthCredentialsProviderBuilder()
                .authorizationServerUrl(issuerUrl + "/protocol/openid-connect/token")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .audience("zeebe-api")
                .build();

        return ZeebeClient.newClientBuilder()
                .gatewayAddress(gatewayAddress)
                .usePlaintext()
                .credentialsProvider(credentialsProvider)
                .defaultJobTimeout(Duration.ofSeconds(30))
                .defaultRequestTimeout(Duration.ofSeconds(30))
                .build();
    }
}
