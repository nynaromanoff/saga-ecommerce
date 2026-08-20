package com.nynaromanoff.inventory_service.dto;

import java.io.Serializable;

public record ItemDTO (
        String productSku,
        Integer quantity
)implements Serializable {}