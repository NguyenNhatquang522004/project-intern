package com.example.demo.common.config;

import io.camunda.zeebe.client.ZeebeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZeebeConfig {

    // @Value("${zeebe.client.broker.gateway-address}")
    // private String gatewayAddress;

    // @Bean
    // public ZeebeClient zeebeClient() {
    //     return ZeebeClient.newClientBuilder()
    //             .gatewayAddress(gatewayAddress)
    //             .usePlaintext()
    //             .build();
    // }
}