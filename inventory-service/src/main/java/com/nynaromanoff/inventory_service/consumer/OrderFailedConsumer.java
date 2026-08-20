package com.nynaromanoff.inventory_service.consumer;

import com.nynaromanoff.inventory_service.dto.OrderCreatedEvent;
import com.nynaromanoff.inventory_service.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderFailedConsumer {

    private final InventoryService inventoryService;

    public OrderFailedConsumer(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "order.failed.inventory-queue", durable = "true"),
            exchange = @Exchange(value = "order.v1.order-failed", type = "fanout")
    ))
    public void consumeOrderFailedEvent(OrderCreatedEvent event) {
        log.info("⚠️ [Inventory] Evento de falha recebido! Iniciando SAGA COMPENSATÓRIA para o pedido ID: {}", event.orderId());
        inventoryService.returnStockFromFailedOrder(event);
    }
}