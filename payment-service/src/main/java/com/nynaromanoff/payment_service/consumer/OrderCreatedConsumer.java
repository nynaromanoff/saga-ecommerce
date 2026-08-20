package com.nynaromanoff.payment_service.consumer;

import com.nynaromanoff.payment_service.dto.OrderCreatedEvent;
import com.nynaromanoff.payment_service.dto.PaymentProcessedEvent;
import com.nynaromanoff.payment_service.producer.PaymentProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class OrderCreatedConsumer {

    private final PaymentProducer paymentProducer;

    public OrderCreatedConsumer(PaymentProducer paymentProducer) {
        this.paymentProducer = paymentProducer;
    }

    @RabbitListener(queues = "payment.v1.receive-order")
    public void consumeOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("💳 [Payment] Mensagem de novo pedido recebida. Processando valor: R$ {}", event.totalValue());

        // Regra de negócio fictícia para o portfólio (Simula aprovação/rejeição)
        String status = "APPROVED";
        if (event.totalValue().compareTo(new BigDecimal("10000.00")) > 0) {
            log.warn("⚠️ Limite excedido! Pagamento rejeitado para o pedido: {}", event.orderId());
            status = "REJECTED";
        }

        // Instancia o evento de resposta e envia para a exchange de pagamentos
        PaymentProcessedEvent responseEvent = new PaymentProcessedEvent(event.orderId(), status);
        paymentProducer.sendPaymentProcessedMessage(responseEvent);
    }
}
