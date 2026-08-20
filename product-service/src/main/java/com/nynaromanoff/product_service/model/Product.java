package com.nynaromanoff.product_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Product {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(unique = true)
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;


}
