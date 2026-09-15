package com.nynaromanoff.order_service.dto;

import java.util.List;
import java.util.UUID;

public record CustomerResponse (
        UUID id,
        String firstName,
        String lastName,
        String email,
        Boolean active,
        List<AddressResponse> addresses
) {}
