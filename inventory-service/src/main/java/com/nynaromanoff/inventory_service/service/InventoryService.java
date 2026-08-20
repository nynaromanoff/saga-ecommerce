package com.nynaromanoff.inventory_service.service;

import com.nynaromanoff.inventory_service.dto.OrderCreatedEvent;
import com.nynaromanoff.inventory_service.dto.ProductCreatedEvent;
import com.nynaromanoff.inventory_service.dto.UpdateQuantityDTO;
import com.nynaromanoff.inventory_service.model.ProductInventory;
import com.nynaromanoff.inventory_service.repository.InventoryRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private final InventoryRepository repository;

    public InventoryService(InventoryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void consumerOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Processando baixa de estoque para o pedido ID: {}", event.orderId());

        if (event.items() == null || event.items().isEmpty()) {
            log.warn("O evento do pedido {} chegou sem nenhum item na lista. Abortando processamento.", event.orderId());
            return;
        }

        for (var item : event.items()) {
            log.info("Buscando SKU [{}] no banco para abater {} unidade(s)", item.productSku(), item.quantity());

            repository.findByProductSkuIgnoreCase(item.productSku())
                    .ifPresentOrElse(inventory -> {
                        int novaQuantidade = inventory.getAvailableQuantity() - item.quantity();
                        inventory.setAvailableQuantity(novaQuantidade);
                        repository.save(inventory);
                        log.info("✅ SUCESSO: SKU {} atualizado. Novo estoque: {}", inventory.getProductSku(), novaQuantidade);
                    }, () -> log.error("❌ ERRO: O SKU [{}] não foi encontrado na tabela tb_inventory!", item.productSku()));
        }
    }

    @Transactional
    public void initializeProductInventory(ProductCreatedEvent event) {
        if (repository.findByProductSku(event.sku()).isPresent()) {
            return;
        }

        ProductInventory inventory = ProductInventory.builder()
                .productSku(event.sku().toUpperCase())
                .availableQuantity(0)
                .build();

        repository.save(inventory);
    }

    @Transactional
    public Optional<ProductInventory> updateProductQuantity(String sku, UpdateQuantityDTO dto) {
        return repository.findByProductSku(sku.toUpperCase())
                .map(inventory -> {
                    inventory.setAvailableQuantity(dto.quantity()); // Atualiza a quantidade com o valor vindo da requisição
                    return repository.save(inventory); // Salva no banco de dados
                });
    }

    @Transactional
    public void returnStockFromFailedOrder(OrderCreatedEvent event) {
        if (event.items() == null || event.items().isEmpty()) return;

        for (var item : event.items()) {
            repository.findByProductSkuIgnoreCase(item.productSku())
                    .ifPresent(inventory -> {
                        Integer estoqueEstornado =
                                inventory
                                        .getAvailableQuantity()
                                        + item.quantity();
                        inventory.setAvailableQuantity(estoqueEstornado);
                        repository.save(inventory);

                        log.info("🔄 [Saga Compensatória] Estoque REVERTIDO com sucesso para o SKU: {}. Novo saldo: {}",
                                inventory.getProductSku(), estoqueEstornado);
                    });
        }
    }
}

