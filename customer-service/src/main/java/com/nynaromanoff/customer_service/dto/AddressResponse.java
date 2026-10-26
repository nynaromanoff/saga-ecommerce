package com.nynaromanoff.customer_service.dto;

public record AddressResponse(
        String zipCode,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state
) {}
