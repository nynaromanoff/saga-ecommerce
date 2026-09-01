package com.nynaromanoff.product_service.dto;

import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

public record ProductRequest (
        String sku,
        String name,
        String description,
        MultipartFile imageUrl,
        BigDecimal price
){}
