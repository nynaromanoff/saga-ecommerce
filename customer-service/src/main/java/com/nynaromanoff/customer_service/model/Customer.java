package com.nynaromanoff.customer_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tb_customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 300)
    private String lastName;

    @Email
    @Column(nullable = false, length = 100)
    private String email;

    @NotBlank(message = "O CPF não pode estar vazio!")
    @Column(nullable = false, length = 11)
    private String cpf;

    private String phone;

    @Column(nullable = false)
    private Boolean active;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "tb_customer_addresses",
            joinColumns = @JoinColumn(name = "customer_id"))
    @Builder.Default
    private List<Address> addresses = new ArrayList<>();

}
