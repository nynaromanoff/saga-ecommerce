package com.nynaromanoff.inventory_service.repository;

import com.nynaromanoff.inventory_service.model.ProductInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<ProductInventory, UUID> {
    Optional<ProductInventory> findByProductSku(String productSku);
    Optional<ProductInventory> findByProductSkuIgnoreCase(String productSku);
}
