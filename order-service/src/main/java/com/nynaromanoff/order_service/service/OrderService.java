package com.nynaromanoff.order_service.service;

import com.nynaromanoff.order_service.dto.*;
import com.nynaromanoff.order_service.model.Order;
import com.nynaromanoff.order_service.model.OrderItem;
import com.nynaromanoff.order_service.model.OrderStatus;
import com.nynaromanoff.order_service.producer.OrderProducer;
import com.nynaromanoff.order_service.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import static java.math.BigDecimal.valueOf;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository repository;
    private final OrderProducer orderProducer;
    private final RestClient productRestClient;

    public OrderService(OrderRepository repository, OrderProducer orderProducer, RestClient productRestClient) {
        this.repository = repository;
        this.orderProducer = orderProducer;
        this.productRestClient = productRestClient;
    }

    public Order createOrder(OrderRequest request) {
        log.info("Iniciando processamento de novo pedido com {} item(ns)...", request.getItems().size());
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalOrderValue = BigDecimal.ZERO;

        try {
            for (ItemDTO item : request.getItems()) {
                log.info("Validando SKU [{}] no catálogo de produtos...", item.getProductSku());

                ProductResponse produto = productRestClient.get()
                        .uri("/{sku}", item.getProductSku().toUpperCase())
                        .retrieve()
                        .body(ProductResponse.class);

                if (produto == null) {
                    throw new IllegalArgumentException("Produto inválido ou nulo no catálogo.");
                }

                BigDecimal itemTotal = produto.getPrice().multiply(valueOf(item.getQuantity()));
                totalOrderValue = totalOrderValue.add(itemTotal);

                orderItems.add(OrderItem.builder()
                        .productSku(item.getProductSku().toUpperCase())
                        .quantity(item.getQuantity())
                        .price(produto.getPrice())
                        .build());
            }

        Order order = Order.builder()
                .items(orderItems)
                .totalValue(totalOrderValue)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        repository.save(order);
        log.info("Pedido registrado com sucesso ID: {}. Status: PENDING", order.getId());

        List<ItemDTO> itensFila = order.getItems()
                .stream()
                .map(item -> new ItemDTO(item.getProductSku(), item.getQuantity()))
                .toList();

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(order.getId())
                .items(itensFila)
                .totalValue(order.getTotalValue())
                .build();

        orderProducer.sendOrderCreatedMessage(event);
        log.info("Evento da Saga publicado com {} itens para processamento distribuído.", itensFila.size());

        return order;
    }catch(HttpClientErrorException.NotFound e){
        log.error("⚠️ COMPRA REJEITADA: Produto inexistente no catálogo.");
        throw new IllegalArgumentException("Produto inválido ou inexistente no catálogo.");
    }

    }

    @Transactional
    public void updateOrderStatus(PaymentProcessedEvent event) {
        repository.findById(event.orderId())
                .ifPresentOrElse(order -> {
                    if ("APPROVED".equalsIgnoreCase(event.paymentStatus())) {
                        order.setStatus(OrderStatus.APPROVED);
                        repository.save(order);
                        log.info("✅ Pedido ID {} APROVADO.", order.getId());
                    } else {
                        order.setStatus(OrderStatus.CANCELED);
                        repository.save(order);
                        log.warn("❌ Pedido ID {} CANCELADO por rejeição do cartão.", order.getId());

                        List<ItemDTO> itensFila = order.getItems().stream()
                                .map(item -> new ItemDTO(item.getProductSku(), item.getQuantity()))
                                .toList();

                        OrderCreatedEvent failedEvent = OrderCreatedEvent.builder()
                                .orderId(order.getId())
                                .items(itensFila)
                                .build();

                        orderProducer.sendOrderFailedMessage(failedEvent); // Publica na exchange de falhas
                    }
                }, () -> log.error("Pedido não encontrado"));
    }
}
