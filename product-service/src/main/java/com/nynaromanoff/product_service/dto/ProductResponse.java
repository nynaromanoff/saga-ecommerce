package com.nynaromanoff.product_service.dto;

import java.math.BigDecimal;

public record ProductResponse(
        String sku,
        String name,
        String description,
        String imageUrl,
        BigDecimal price
) {}
