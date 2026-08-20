package com.nynaromanoff.order_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {
    @Id @UuidGenerator
    private UUID id;

    private String productSku;
    private Integer quantity;
    private BigDecimal price;
}
