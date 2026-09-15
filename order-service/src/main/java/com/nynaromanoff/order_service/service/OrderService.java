package com.nynaromanoff.order_service.service;

import com.nynaromanoff.order_service.client.CustomerClient;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
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
    private final CustomerClient  customerClient;

    public OrderService(OrderRepository repository, OrderProducer orderProducer, RestClient productRestClient, CustomerClient customerClient) {
        this.repository = repository;
        this.orderProducer = orderProducer;
        this.productRestClient = productRestClient;
        this.customerClient = customerClient;
    }

    public Order createOrder(OrderRequest request, Jwt jwt) {
        log.info("Iniciando processamento de novo pedido com {} item(ns)...", request.items().size());
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalOrderValue = BigDecimal.ZERO;

        CustomerResponse customer = null;

        try {
            try {
                customer = customerClient.getCurrentCustomer();
                log.info("👤 [Customer Service] Cliente localizado: {}", customer.firstName());
            } catch (Exception e) {
                log.error("❌ [Customer Service] Falha crítica na chamada do Feign para o ID: {}. Erro original: ","", e);
                throw new IllegalArgumentException("Não foi possível gerar o pedido. O cliente informado não existe no ecossistema ou a comunicação falhou.");
            }

            if (!customer.active()) {
                log.warn("⚠️ COMPRA REJEITADA: O cliente {} está inativo no sistema.", customer.firstName());
                throw new IllegalStateException("A conta do cliente está inativa. Operação bloqueada.");
            }

            if (customer.addresses() == null || customer.addresses().isEmpty()) {
                log.warn("⚠️ COMPRA REJEITADA: O cliente {} não possui nenhum endereço de entrega cadastrado.", customer.firstName());
                throw new IllegalArgumentException("Não é possível fechar o pedido. Cadastre um endereço de entrega primeiro.");
            }

            AddressResponse entrega = customer.addresses().get(0);
            log.info("📦 [OrderService] Endereço de entrega selecionado: {}, Nº {}", entrega.street(), entrega.number());

            for (ItemDTO item : request.items()) {
                log.info("Validando SKU [{}] no catálogo de produtos...", item.getProductSku());

                try{

                    ResponseEntity<ProductResponse> response = productRestClient.get()
                            .uri("/{sku}", item.getProductSku().toUpperCase())
                            .retrieve()
                            .toEntity(ProductResponse.class);

                    log.info("📡 [Catálogo API] Resposta HTTP recebida. Status: {}, Corpo: {}", response.getStatusCode(), response.getBody());

                    ProductResponse produto = response.getBody();

                    if (produto == null) {
                        throw new IllegalArgumentException("Produto inválido ou nulo no catálogo.");
                    }

                    BigDecimal itemTotal = produto.price().multiply(BigDecimal.valueOf(item.getQuantity()));
                    totalOrderValue = totalOrderValue.add(itemTotal);

                    orderItems.add(OrderItem.builder()
                            .productSku(item.getProductSku().toUpperCase())
                            .quantity(item.getQuantity())
                            .price(produto.price())
                            .build());
                } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
                    log.error("❌ [404] O SKU {} realmente não existe no banco de dados do catálogo!", item.getProductSku());
                    throw new IllegalArgumentException("Produto inexistente no catálogo.");
                }
            }

            Order order = Order.builder()
                    .customerId(customer.id())
                    .deliveryStreet(entrega.street())
                    .deliveryNumber(entrega.number())
                    .deliveryZipCode(entrega.zipCode())
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

        } catch (HttpClientErrorException.NotFound e) {
            log.error("❌ [Catálogo] Produto não encontrado no microsserviço de produtos: ", e);
            throw new IllegalArgumentException("Produto inválido ou inexistente no catálogo.");
        } catch (Exception e) {
            log.error("❌ [Erro Genérico] Falha inesperada no processamento da ordem: ", e);
            throw e;
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
