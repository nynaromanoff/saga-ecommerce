package com.nynaromanoff.product_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String PRODUCT_EXCHANGE = "product.v1.product-created";
    public static final String INVENTORY_PRODUCT_QUEUE = "inventory.product";


    @Bean
    public FanoutExchange productExchange() {
        return new FanoutExchange(PRODUCT_EXCHANGE);
    }
    @Bean
    public Queue inventoryProductQueue() {
        return new Queue(INVENTORY_PRODUCT_QUEUE, true); // true = fila durável (não some ao reiniciar o RabbitMQ)
    }

    @Bean
    public Binding bindingInventory() {
        return BindingBuilder.bind(inventoryProductQueue()).to(productExchange());
    }

    @Bean
    public JacksonJsonMessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
