package com.nynaromanoff.product_service.dto;

import java.io.Serializable;
import java.util.UUID;

public record ProductCreatedEvent(
        UUID id,
        String sku,
        String name
) implements Serializable {}