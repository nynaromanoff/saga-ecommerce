package com.nynaromanoff.customer_service.dto;

public record CustomerRequest(
        String firstName,
        String lastName,
        String email,
        String cpf,
        String phone,
        Boolean active) {
}