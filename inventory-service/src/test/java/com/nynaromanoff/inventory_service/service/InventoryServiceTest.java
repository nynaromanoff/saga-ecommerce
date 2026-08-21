package com.nynaromanoff.inventory_service.service;


import com.nynaromanoff.inventory_service.dto.ItemDTO;
import com.nynaromanoff.inventory_service.dto.OrderCreatedEvent;
import com.nynaromanoff.inventory_service.model.ProductInventory;
import com.nynaromanoff.inventory_service.repository.InventoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    @DisplayName("Deve dar baixa no estoque com sucesso ao receber evento de pedido criado")
    void deductStockFromOrder_Success() {
        UUID orderId = UUID.randomUUID();
        ItemDTO itemFila = new ItemDTO("NOTE-GAMER-01", 3);
        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId,
                List.of(itemFila),
                new BigDecimal("15000.00")
        );

        ProductInventory estoqueDeduct  = ProductInventory.builder()
                .id(orderId)
                .productSku("NOTE-GAMER-01")
                .availableQuantity(10)
                .build();

        when(inventoryRepository.findByProductSkuIgnoreCase("NOTE-GAMER-01")).thenReturn(Optional.of(estoqueDeduct));
        inventoryService.consumerOrderCreatedEvent(event);
        assertEquals(7, estoqueDeduct.getAvailableQuantity());
        verify(inventoryRepository, times(1)).save(estoqueDeduct);
    }

    @Test
    @DisplayName("Deve reverter e estornar estoque com sucesso durante a Saga Compensatória")
    void returnStockFromFailedOrder_Success() {
        UUID orderId = UUID.randomUUID();
        ItemDTO itemFalho = new ItemDTO("NOTE-GAMER-01", 3); // Devolvendo 3 unidades

        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId,
                List.of(itemFalho),
                new BigDecimal("15000.00")
        );

        ProductInventory estoqueReturn  = ProductInventory.builder()
                .id(orderId)
                .productSku("NOTE-GAMER-01")
                .availableQuantity(10)
                .build();

        when(inventoryRepository.findByProductSkuIgnoreCase("NOTE-GAMER-01")).thenReturn(Optional.of(estoqueReturn ));
        inventoryService.returnStockFromFailedOrder(event);
        assertEquals(7, estoqueReturn .getAvailableQuantity());
        verify(inventoryRepository, times(1)).save(estoqueReturn );
    }
}