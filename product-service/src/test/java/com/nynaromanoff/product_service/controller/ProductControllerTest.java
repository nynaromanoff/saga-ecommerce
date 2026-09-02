package com.nynaromanoff.product_service.controller;

import com.nynaromanoff.product_service.dto.ProductRequest;
import com.nynaromanoff.product_service.dto.ProductResponse;
import com.nynaromanoff.product_service.model.Product;
import com.nynaromanoff.product_service.repository.ProductRepository;
import com.nynaromanoff.product_service.service.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {
    @Mock
    private ProductRepository productRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private ProductController productController;

    @Test
    @DisplayName("Deve cadastrar produto com sucesso e retornar 21 Created usando partes separadas")
    void createProduct_Success() throws Exception {

        MockMultipartFile arquivoMock = new MockMultipartFile(
                "file",
                "playstation5.jpg",
                "image/jpeg",
                "binarios-falsos-de-imagem-para-teste".getBytes()
        );

        ProductRequest requestDto = new ProductRequest(
                "PS5-SLIM-01",
                "Console Sony PlayStation 5 Slim",
                "Console Sony PlayStation 5 Slim, moderno",
                arquivoMock,
                new BigDecimal("3989.05")
        );


        String urlFicticiaS3 = "https://amazonaws.com";
        when(storageService.uploadFile(any())).thenReturn(urlFicticiaS3);

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product produtoSalvo = invocation.getArgument(0);
            produtoSalvo.setId(UUID.randomUUID());
            return produtoSalvo;
        });


        ResponseEntity<ProductResponse> resposta = productController.create(requestDto, arquivoMock);

        assertNotNull(resposta);
        assertEquals(HttpStatus.CREATED, resposta.getStatusCode()); // Valida o retorno HTTP 201

        ProductResponse response = resposta.getBody();
        assertNotNull(response);
        assertEquals("PS5-SLIM-01", response.getSku());
        assertEquals("Console Sony PlayStation 5 Slim", response.getName());
        assertEquals("Console Sony PlayStation 5 Slim, Moderno", response.getDescription());
        assertEquals(new BigDecimal("3989.05"), response.getPrice());

        verify(storageService, times(1)).uploadFile(arquivoMock);

        verify(productRepository, times(1)).save(any(Product.class));
    }

}