package com.nynaromanoff.order_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Bean
    public RestClient productRestClient() {
        return  RestClient.builder()
                .baseUrl("http://localhost:8084/api/v1/products")
                .build();
    }
}
