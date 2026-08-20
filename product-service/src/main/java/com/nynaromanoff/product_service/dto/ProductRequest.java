package com.nynaromanoff.product_service.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProductRequest {
    private String sku;
    private String name;
    private String description;
    private String imageUrl;
    private BigDecimal price;
}
