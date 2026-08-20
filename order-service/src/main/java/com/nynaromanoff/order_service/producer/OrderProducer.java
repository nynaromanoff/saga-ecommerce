package com.nynaromanoff.order_service.producer;

import com.nynaromanoff.order_service.config.RabbitMQConfig;
import com.nynaromanoff.order_service.dto.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderProducer {
    private final RabbitTemplate rabbitTemplate;

    public OrderProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendOrderCreatedMessage(OrderCreatedEvent event) {
        log.info("📢 [RabbitMQ] Disparando evento para a exchange dedicada de pedidos. ID do Pedido: {}", event.getOrderId());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                "",
                event
        );
    }

    public void sendOrderFailedMessage(OrderCreatedEvent event) {
        log.info("⚠️ [RabbitMQ] Publicando falha do pedido na exchange compensatória. ID: {}", event.getOrderId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_FAILED_EXCHANGE,
                "",
                event);
    }
}
