package com.nynaromanoff.order_service.controller;

import com.nynaromanoff.order_service.dto.OrderRequest;
import com.nynaromanoff.order_service.model.Order;
import com.nynaromanoff.order_service.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> create(@RequestBody OrderRequest request,
                                        @AuthenticationPrincipal Jwt jwt) {
        Order order = orderService.createOrder(request, jwt);

        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
}