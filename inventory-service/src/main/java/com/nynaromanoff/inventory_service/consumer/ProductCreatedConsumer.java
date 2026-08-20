package com.nynaromanoff.inventory_service.consumer;

import com.nynaromanoff.inventory_service.dto.ProductCreatedEvent;
import com.nynaromanoff.inventory_service.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProductCreatedConsumer {

    private final InventoryService inventoryService;

    public ProductCreatedConsumer(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "inventory.v1.sync-product", durable = "true"),
            exchange = @Exchange(value = "product.v1.product-created", type = "fanout") // Mudado para fanout e nome dedicado
            // Removemos a propriedade 'key' pois fanout faz broadcast automático
    ))
    public void consumeProductCreatedEvent(ProductCreatedEvent event) {
        log.info("Mensagem recebida na fila de Inventário para o SKU: {}", event.sku());

        try {
            // Envia para a camada de serviço para inicializar o estoque do novo produto
            inventoryService.initializeProductInventory(event);
        } catch (Exception e) {
            log.error("Erro crítico ao processar evento de sincronização de produto: {}", event.sku(), e);
            // Em produção, aqui trataríamos rejeições ou enviaríamos para uma DLQ (Dead Letter Queue)
        }
    }
}