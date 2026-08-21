package com.nynaromanoff.payment_service.consumer;

import com.nynaromanoff.payment_service.dto.OrderCreatedEvent;
import com.nynaromanoff.payment_service.dto.PaymentProcessedEvent;
import com.nynaromanoff.payment_service.model.Payment;
import com.nynaromanoff.payment_service.model.PaymentStatus;
import com.nynaromanoff.payment_service.producer.PaymentProducer;
import com.nynaromanoff.payment_service.repository.PaymentRepository;
import com.nynaromanoff.payment_service.service.PaymentService;
import com.nynaromanoff.payment_service.service.StripeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Slf4j
public class OrderCreatedConsumer {

    private final PaymentProducer paymentProducer;
    private final PaymentRepository repository;
    private final StripeService stripeService;

    public OrderCreatedConsumer(PaymentProducer paymentProducer, PaymentRepository repository, StripeService stripeService) {
        this.paymentProducer = paymentProducer;
        this.repository = repository;
        this.stripeService = stripeService;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "payment.v1.receive-order", durable = "true"),
            exchange = @Exchange(value = "order.v1.order-created", type = "fanout")
    ))
    public void consumeOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("💳 [Payment] Iniciando transação externa na Stripe para o Pedido ID: {}", event.orderId());

        Boolean isApproved = stripeService.processCardPayment(
                event.totalValue(),
                event.orderId().toString());

        PaymentStatus status = isApproved ? PaymentStatus.APPROVED : PaymentStatus.REJECTED;

        Payment payment = Payment.builder()
                .orderId(event.orderId())
                .value(event.totalValue())
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
        repository.save(payment);

        // Instancia o evento de resposta e envia para a exchange de pagamentos
        PaymentProcessedEvent responseEvent = new PaymentProcessedEvent(event.orderId(), status.name());
        paymentProducer.sendPaymentProcessedMessage(responseEvent);
    }
}
