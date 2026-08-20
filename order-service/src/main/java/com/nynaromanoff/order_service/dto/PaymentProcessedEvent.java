package com.nynaromanoff.order_service.dto;

import java.io.Serializable;
import java.util.UUID;

public record PaymentProcessedEvent(
        UUID orderId,
        String paymentStatus // APPROVED ou REJECTED
) implements Serializable {}