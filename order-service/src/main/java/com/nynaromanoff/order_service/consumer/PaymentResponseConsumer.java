package com.nynaromanoff.order_service.consumer;

import com.nynaromanoff.order_service.config.RabbitMQConfig;
import com.nynaromanoff.order_service.dto.PaymentProcessedEvent;
import com.nynaromanoff.order_service.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentResponseConsumer {
    private final OrderService orderService;

    public PaymentResponseConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = RabbitMQConfig.ORDER_PAYMENT_RESPONSE_QUEUE, durable = "true"),
            exchange = @Exchange(value = RabbitMQConfig.PAYMENT_EXCHANGE, type = "fanout")
    ))
    public void consumePaymentResponse(PaymentProcessedEvent event) {
        log.info("💳 [Order] Resposta de pagamento recebida para o Pedido ID: {}. Status: {}",
                event.orderId(), event.paymentStatus());

        // Encaminha para a camada de serviço atualizar o banco
        orderService.updateOrderStatus(event);
    }
}
