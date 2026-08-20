package com.nynaromanoff.inventory_service.consumer;

import com.nynaromanoff.inventory_service.config.RabbitMQConfig;
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
public class OrderCreatedConsumer {
    private final InventoryService inventoryService;

    public OrderCreatedConsumer(InventoryService inventoryService, RabbitMQConfig rabbitMQConfig) {
        this.inventoryService = inventoryService;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = RabbitMQConfig.ORDER_QUEUE, durable = "true"),
            exchange = @Exchange (value = RabbitMQConfig.ORDER_EXCHANGE, type = "fanout") // Mudado para fanout e nome dedicado
    ))
    public void consumeOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("📢 Nova mensagem de venda recebida! Processando baixa para o Pedido ID: {}", event.orderId());
        inventoryService.consumerOrderCreatedEvent(event);
    }
}
