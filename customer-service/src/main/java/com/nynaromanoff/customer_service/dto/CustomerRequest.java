package com.nynaromanoff.customer_service.dto;

import com.nynaromanoff.customer_service.model.Address;

import java.util.List;

public record CustomerRequest(
        String firstName,
        String lastName,
        String email,
        String cpf,
        String phone,
        String password,
        Boolean active,
        List<Address> address) {
}