package com.nynaromanoff.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    private String productSku;
    private Integer quantity;
    private List<ItemDTO> items;
    private BigDecimal totalValue;
    private UUID customerId;
}