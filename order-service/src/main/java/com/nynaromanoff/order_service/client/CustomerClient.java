package com.nynaromanoff.order_service.client;

import com.nynaromanoff.order_service.dto.CustomerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "customer-service", url = "http://localhost:8086/api/v1/customers")
public interface CustomerClient {

    @GetMapping("/me")
    CustomerResponse getCurrentCustomer();

    @GetMapping("/{id}")
    CustomerResponse getCustomerById(@PathVariable("id") UUID id);
}
