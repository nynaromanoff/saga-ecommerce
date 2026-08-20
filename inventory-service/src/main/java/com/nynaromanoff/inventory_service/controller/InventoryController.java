package com.nynaromanoff.inventory_service.controller;

import com.nynaromanoff.inventory_service.dto.UpdateQuantityDTO;
import com.nynaromanoff.inventory_service.model.ProductInventory;
import com.nynaromanoff.inventory_service.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService service;

    public InventoryController( InventoryService service) {
        this.service = service;
    }

    @PatchMapping   ("/{sku}/refil")
    public ResponseEntity<ProductInventory> updateQuantity(
            @PathVariable String sku,
            @RequestBody @Valid UpdateQuantityDTO request) {

        return service.updateProductQuantity(sku, request)
                .map(inventory -> ResponseEntity.ok(inventory)) // Retorna 200 OK com o JSON atualizado
                .orElse(ResponseEntity.notFound().build());    // Retorna 404 caso o SKU não exista
    }
}
