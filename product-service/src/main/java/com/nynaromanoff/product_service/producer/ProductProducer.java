package com.nynaromanoff.product_service.producer;


import com.nynaromanoff.product_service.config.RabbitMQConfig;
import com.nynaromanoff.product_service.dto.ProductCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProductProducer {
    private final RabbitTemplate rabbitTemplate;

    public ProductProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendProductCreatedMessage(ProductCreatedEvent event) {
        log.info("Disparando evento de produto criado para o RabbitMQ: {}", event.sku());

        rabbitTemplate.convertAndSend(RabbitMQConfig.PRODUCT_EXCHANGE, "", event);
    }
}
