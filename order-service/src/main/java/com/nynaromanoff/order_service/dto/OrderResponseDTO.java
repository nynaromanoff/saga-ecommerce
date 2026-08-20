package com.nynaromanoff.order_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderResponseDTO(
        UUID id,                  // ID do pedido gerado pelo PostgreSQL
        String customerId,        // Identificador do cliente que comprou
        BigDecimal totalValue,    // Valor total somado do pedido
        String status,            // Status atual (ex: PENDING, APPROVED, CANCELED)
        LocalDateTime createdAt // Data e hora em que o pedido foi feito
) {}