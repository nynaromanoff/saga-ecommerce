package com.nynaromanoff.payment_service.service;

import com.nynaromanoff.payment_service.config.RabbitMQConfig;
import com.nynaromanoff.payment_service.dto.OrderCreatedEvent;
import com.nynaromanoff.payment_service.dto.PaymentProcessedEvent;
import com.nynaromanoff.payment_service.model.Payment;
import com.nynaromanoff.payment_service.model.PaymentStatus;
import com.nynaromanoff.payment_service.producer.PaymentProducer;
import com.nynaromanoff.payment_service.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentRepository repository;
    private final PaymentProducer paymentProducer;

    public PaymentService(PaymentRepository repository, PaymentProducer paymentProducer) {
        this.repository = repository;
        this.paymentProducer = paymentProducer;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = RabbitMQConfig.PAYMENT_ORDER_QUEUE, durable = "true"),
            exchange = @Exchange (value = RabbitMQConfig.ORDER_EXCHANGE, type = "fanout")
    ))
    public void consumeOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Processando pagamento para o pedido ID: {} no valor de R$ {}",
                event.orderId(), event.orderId());

        PaymentStatus status = PaymentStatus.APPROVED;
        if (event.totalValue().compareTo(new BigDecimal("10000.00")) > 0) {
            status = PaymentStatus.REFUNDED;
        }

        Payment payment = Payment.builder()
                .orderId(event.orderId())
                .value(event.totalValue())
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();

        repository.save(payment);
        log.info("Pagamento aprovado com sucesso para o pedido ID: {}", event.orderId());

        PaymentProcessedEvent responseEvent = new PaymentProcessedEvent(
                event.orderId(),
                status.name());
        paymentProducer.sendPaymentProcessedMessage(responseEvent);
    }
}
