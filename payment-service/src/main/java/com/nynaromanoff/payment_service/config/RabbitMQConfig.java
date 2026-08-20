package com.nynaromanoff.payment_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String ORDER_EXCHANGE = "order.v1.order-created";
    public static final String PAYMENT_ORDER_QUEUE = "payment.v1.receive-order";
    public static final String PAYMENT_EXCHANGE = "payment.v1.payment-processed";

    @Bean
    public FanoutExchange orderExchange() {
        return new FanoutExchange(ORDER_EXCHANGE);
    }

    @Bean
    public FanoutExchange paymentExchange() {
        return new FanoutExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public Queue paymentOrderQueue() {
        return new Queue(PAYMENT_ORDER_QUEUE, true);
    }

    // Amarração: Prende a fila do pagamento na exchange de pedidos
    @Bean
    public Binding bindingPaymentOrder() {
        return BindingBuilder.bind(paymentOrderQueue()).to(orderExchange());
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
