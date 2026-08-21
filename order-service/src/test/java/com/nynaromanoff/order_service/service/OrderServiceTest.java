package com.nynaromanoff.order_service.service;


import com.nynaromanoff.order_service.dto.ItemDTO;
import com.nynaromanoff.order_service.dto.OrderRequest;
import com.nynaromanoff.order_service.dto.ProductResponse;
import com.nynaromanoff.order_service.model.Order;
import com.nynaromanoff.order_service.model.OrderStatus;
import com.nynaromanoff.order_service.producer.OrderProducer;
import com.nynaromanoff.order_service.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {
    @Mock
    private OrderRepository repository;

    @Mock
    private  RestClient productRestClient;

    @Mock
    private OrderProducer orderProducer;

    @InjectMocks
    private OrderService orderService;
    @Test
    @DisplayName("Deve criar um pedido com sucesso quando o produto for válido no catálogo")
    void createOrder_Success() {
        ItemDTO itemRequest = new ItemDTO("NOTE-GAMER-01", 2);
        OrderRequest request = new OrderRequest(
                "cliente-teste-id",
                2,
                List.of(itemRequest),
                new BigDecimal("10000.00")
        );

        ProductResponse mockProduto = new ProductResponse("NOTE-GAMER-01", "Notebook Asus", "Testando", new BigDecimal("5000.00"));

        RestClient.RequestHeadersUriSpec<?> mockUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec<?> mockHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);

        doReturn(mockUriSpec).when(productRestClient).get();
        doReturn(mockHeadersSpec).when(mockUriSpec).uri(anyString(), any(Object[].class));
        doReturn(mockResponseSpec).when(mockHeadersSpec).retrieve();
        doReturn(mockProduto).when(mockResponseSpec).body(ProductResponse.class);

        when(repository.save(any(Order.class))).thenAnswer(invocation -> {
            Order orderSalva = invocation.getArgument(0);
            orderSalva.setId(java.util.UUID.randomUUID()); // Simula o ID gerado pelo Postgres
            return orderSalva;
        });

        Order resultado = orderService.createOrder(request);

        assertNotNull(resultado);
        assertNotNull(resultado.getId());
        assertEquals(OrderStatus.PENDING, resultado.getStatus());

        assertEquals(new BigDecimal("10000.00"), resultado.getTotalValue());
        assertEquals(1, resultado.getItems().size());

        verify(repository, times(1)).save(any(Order.class));
        verify(orderProducer, times(1)).sendOrderCreatedMessage(any());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando o produto não for encontrado no catálogo")
    void createOrder_ProductNotFound_ThrowsException() {
        ItemDTO itemRequest = new ItemDTO("PRODUTO-INEXISTENTE", 1);

        OrderRequest request = new OrderRequest(
                "cliente-teste-id",
                1,
                List.of(itemRequest),
                new BigDecimal("100.00")
        );

        RestClient.RequestHeadersUriSpec<?> mockUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec<?> mockHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);

        doReturn(mockUriSpec).when(productRestClient).get();
        doReturn(mockHeadersSpec).when(mockUriSpec).uri(anyString(), any(Object[].class));
        doReturn(mockResponseSpec).when(mockHeadersSpec).retrieve();

        doReturn(null).when(mockResponseSpec).body(ProductResponse.class);

        IllegalArgumentException excecao = assertThrows(IllegalArgumentException.class, () -> {
            orderService.createOrder(request);
        });

        assertEquals("Produto inválido ou nulo no catálogo.", excecao.getMessage());

        verify(repository, never()).save(any(com.nynaromanoff.order_service.model.Order.class));

        verify(orderProducer, never()).sendOrderCreatedMessage(any());
    }
}
