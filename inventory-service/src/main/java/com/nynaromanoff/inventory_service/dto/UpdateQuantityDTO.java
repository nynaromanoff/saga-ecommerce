package com.nynaromanoff.inventory_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateQuantityDTO(@NotNull(message = "A quantidade é obrigatória")
                                @Min(value = 0, message = "A quantidade de estoque não pode ser negativa")
                                Integer quantity) {
}
