package com.nynaromanoff.payment_service.dto;

import java.io.Serializable;
import java.util.UUID;

public record PaymentProcessedEvent(
        UUID orderId,
        String paymentStatus
) implements Serializable {}
