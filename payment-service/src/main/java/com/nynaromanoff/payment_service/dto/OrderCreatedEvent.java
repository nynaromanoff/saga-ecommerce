package com.nynaromanoff.payment_service.dto;

import java.math.BigDecimal;
import java.io.Serializable;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        BigDecimal totalValue
) implements Serializable {}
