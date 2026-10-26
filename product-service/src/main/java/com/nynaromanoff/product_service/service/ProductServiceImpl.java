package com.nynaromanoff.product_service.service;

import com.nynaromanoff.product_service.dto.ProductCreatedEvent;
import com.nynaromanoff.product_service.dto.ProductRequest;
import com.nynaromanoff.product_service.dto.ProductResponse;
import com.nynaromanoff.product_service.model.Product;
import com.nynaromanoff.product_service.repository.ProductRepository;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;


@Service
public class ProductServiceImpl implements ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private final ProductRepository repository;
    private final StorageService storage;
    private final RabbitTemplate rabbitTemplate;

    public ProductServiceImpl(ProductRepository repository, StorageService storage, RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.storage = storage;
        this.rabbitTemplate = rabbitTemplate;
    }


    @Transactional
    public ProductResponse createProduct(ProductRequest request, MultipartFile file) {
        log.info("🏭 [ProductService] Processando regras de negócio para o produto: {}", request.name());

        if(repository.existsBySkuIgnoreCase(request.sku().trim())) {
            throw new IllegalArgumentException("Já existe um produto cadastrado com este SKU no ecossistema.");
        }

        String urlImagemS3 = storage.uploadFile(file);
        log.info("✅ [S3] Upload concluído com sucesso. URL gerada: {}", urlImagemS3);

        Product product = Product.builder()
                .sku(request.sku().toUpperCase().trim())
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .imageUrl(urlImagemS3)
                .build();

        repository.save(product);
        log.info("💾 [Postgres] Produto salvo com sucesso ID: {}", product.getId());

        ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent(product.getSku(), product.getName());
        rabbitTemplate.convertAndSend("product.v1.product-created", "", productCreatedEvent);
        log.info("🐰 [RabbitMQ] Evento enviado para atualização automática de estoque.");

        return new ProductResponse(
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getImageUrl(),
                product.getPrice()
        );

    }

    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        log.info("🔍 [ProductService] Buscando produto pelo SKU: {}", sku);
        return repository.findBySku(sku.toUpperCase().trim())
                .map(p -> new ProductResponse(
                        p.getSku(),
                        p.getName(),
                        p.getDescription(),
                        p.getImageUrl(),
                        p.getPrice()
                ))
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado com o SKU informado."));
    }

    @Transactional(readOnly = true)
    public Page<Product> listAll(Pageable pageable) {
        log.info("📋 [ProductService] Listando todos os produtos com paginação");
        return repository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Product> searchProductByName(String name, Pageable pageable) {
        log.info("🔎 [ProductService] Buscando produtos contendo o nome: {}", name);
        return repository.findByNameContainingIgnoreCase(name.trim(), pageable);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        log.info("🗑️ [ProductService] Iniciando processo de exclusão do produto ID: {}", id);

        Product product = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Não foi possível excluir. Produto não encontrado com o ID informado."));

        storage.deleteFile(product.getImageUrl());

        repository.delete(product);
        log.info("✅ [ProductService] Produto ID {} e sua imagem correspondente foram excluídos com sucesso.", id);
    }
}
