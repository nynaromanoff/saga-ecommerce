package com.nynaromanoff.inventory_service.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent (
        UUID orderId,
        List<ItemDTO> items,
        BigDecimal totalValue
)implements Serializable {}
