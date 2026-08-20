package com.nynaromanoff.inventory_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PRODUCT_EXCHANGE = "product.v1.product-created";
    public static final String PRODUCT_QUEUE = "inventory.v1.sync-product";

    public static final String ORDER_EXCHANGE = "order.v1.order-created";
    public static final String ORDER_QUEUE = "order.created.inventory-queue";

    @Bean
    public FanoutExchange productExchange() {
        return new FanoutExchange(PRODUCT_EXCHANGE);
    }

    @Bean
    public FanoutExchange orderExchange() {
        return new FanoutExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Queue inventoryProductQueue() {
        return new Queue(PRODUCT_QUEUE, true);
    }

    @Bean
    public Queue orderCreatedInventoryQueue() {
        return new Queue(ORDER_QUEUE, true);
    }

    @Bean
    public Binding bindingInventoryProduct() {
        return BindingBuilder.bind(inventoryProductQueue()).to(productExchange());
    }

    @Bean
    public Binding bindingOrderCreatedInventory() {
        return BindingBuilder.bind(orderCreatedInventoryQueue()).to(orderExchange());
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    @Bean
    public JacksonJsonMessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
