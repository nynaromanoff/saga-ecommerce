package com.nynaromanoff.payment_service.producer;

import com.nynaromanoff.payment_service.config.RabbitMQConfig;
import com.nynaromanoff.payment_service.dto.PaymentProcessedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentProducer {

    private final RabbitTemplate rabbitTemplate;

    public PaymentProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendPaymentProcessedMessage(PaymentProcessedEvent event) {
        log.info("📢 [RabbitMQ] Publicando resultado do pagamento para o Pedido ID: {}. Status: {}",
                event.orderId(), event.paymentStatus());

        rabbitTemplate.convertAndSend(RabbitMQConfig.PAYMENT_EXCHANGE, "", event);
    }
}