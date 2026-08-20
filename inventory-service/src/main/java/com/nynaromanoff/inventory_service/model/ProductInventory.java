package com.nynaromanoff.inventory_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "inventories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductInventory {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(unique = true)
    private String productSku;

    private Integer availableQuantity;
}
