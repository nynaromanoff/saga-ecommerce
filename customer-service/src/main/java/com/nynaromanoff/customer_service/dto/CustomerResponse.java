package com.nynaromanoff.customer_service.dto;

import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String cpf,
        String phone,
        Boolean active) {}