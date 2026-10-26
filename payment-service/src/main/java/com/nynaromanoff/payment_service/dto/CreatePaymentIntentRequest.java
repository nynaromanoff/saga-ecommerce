package com.nynaromanoff.payment_service.dto;

import java.util.UUID;

public record CreatePaymentIntentRequest(
        UUID orderId
) {}
