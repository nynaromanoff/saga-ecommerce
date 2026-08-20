package com.nynaromanoff.order_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String ORDER_EXCHANGE = "order.v1.order-created";
    public static final String ORDER_PAYMENT_RESPONSE_QUEUE = "order.v1.payment-response";
    public static final String PAYMENT_EXCHANGE = "payment.v1.payment-processed";
    public static final String ORDER_FAILED_EXCHANGE = "order.v1.failed-order";

    @Bean
    public FanoutExchange orderExchange() {
        return new FanoutExchange(ORDER_EXCHANGE);
    }

    @Bean
    public FanoutExchange paymentExchange() {
        return new FanoutExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public FanoutExchange orderFailedExchange() {
        return new FanoutExchange(ORDER_FAILED_EXCHANGE);
    }

    @Bean
    public Queue orderPaymentResponseQueue() {
        return new Queue(ORDER_PAYMENT_RESPONSE_QUEUE, true);
    }

    @Bean
    public Binding bindingPayment() {
        return BindingBuilder
                .bind(orderPaymentResponseQueue())
                .to(paymentExchange());
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
