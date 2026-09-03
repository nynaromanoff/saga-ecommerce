package com.nynaromanoff.customer_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {
    @Column(nullable = false, length = 9)
    private String zipCode; // CEP

    @Column(nullable = false, length = 150)
    private String street; // Rua

    @Column(nullable = false, length = 20)
    private String number; // Número

    @Column(length = 100)
    private String complement; // Complemento

    @Column(nullable = false, length = 100)
    private String neighborhood; // Bairro

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 2)
    private String state;
}
