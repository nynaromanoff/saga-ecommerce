
package com.nynaromanoff.payment_service.consumer;

import com.nynaromanoff.payment_service.config.RabbitMQConfig;
import com.nynaromanoff.payment_service.dto.OrderCreatedEvent;
import com.nynaromanoff.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderCreatedConsumer {

    private final PaymentService paymentService;

    public OrderCreatedConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = RabbitMQConfig.PAYMENT_ORDER_QUEUE, durable = "true"),
            exchange = @Exchange(value = RabbitMQConfig.ORDER_EXCHANGE, type = "fanout")
    ))
    public void consumeOrderCreatedEvent(OrderCreatedEvent event) throws Exception {
        log.info("Evento recebido para o pedido {}. Iniciando pagamento.", event.orderId());
        paymentService.processOrderCreatedEvent(event);
    }
}
