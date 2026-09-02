package com.nynaromanoff.product_service.controller;

import com.nynaromanoff.product_service.dto.ProductCreatedEvent;
import com.nynaromanoff.product_service.dto.ProductRequest;
import com.nynaromanoff.product_service.dto.ProductResponse;
import com.nynaromanoff.product_service.model.Product;
import com.nynaromanoff.product_service.producer.ProductProducer;
import com.nynaromanoff.product_service.repository.ProductRepository;
import com.nynaromanoff.product_service.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductRepository repository;
    private final StorageService storage;
    private final ProductProducer productProducer;

    public ProductController(ProductRepository repository, StorageService storage, ObjectMapper objectMapper, ProductProducer productProducer) {
        this.repository = repository;
        this.storage = storage;
        this.productProducer = productProducer;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> create(
            @RequestPart("product") ProductRequest request,
            @RequestPart("file") MultipartFile file) {

        log.info("📢 [ProductAPI] Recebida requisição multipart para cadastrar produto: {} - R$ {}", request.name() , request.price());

        if (file == null || file.isEmpty()) {
            log.warn("⚠️ Falha ao processar: Nenhum arquivo de imagem foi enviado no formulário.");
            return ResponseEntity.badRequest().build();
        }
        try {
            String urlImagemS3 = storage.uploadFile(file);
            log.info("✅ [S3] Upload concluído com sucesso. URL gerada: {}", urlImagemS3);

            Product product = Product.builder()
                    .sku(request.sku())
                    .name(request.name())
                    .description(request.description())
                    .price(request.price())
                    .imageUrl(urlImagemS3) // Salva o link oficial da AWS na tabela
                    .build();

            repository.save(product);
            log.info("💾 [Postgres] Produto salvo no banco de dados com ID: {}", product.getId());

            ProductResponse response = new ProductResponse(
                    product.getSku(),
                    product.getName(),
                    product.getDescription(),
                    product.getPrice()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("❌ Falha crítica ao processar cadastro de produto e upload na AWS", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{sku}")
    public ResponseEntity<ProductResponse> getProductBySku(@PathVariable String sku) {
        return repository.findBySku(sku.toUpperCase())
                .map(p -> new ProductResponse(p.getSku(), p.getName(), p.getDescription(), p.getPrice()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<Page<Product>> listAll(Pageable pageable) {
        Page<Product> products = repository.findAll(pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<Product>> getProductByName(@RequestParam("name") String name,
                                                         Pageable pageable) {
        Page<Product> products = repository.findByNameContainingIgnoreCase(name, pageable);
        return ResponseEntity.ok(products);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("Iniciando processo de exclusão do produto ID: {}", id);

        return repository.findById(id)
                .map(product -> {
                    storage.deleteFile(product.getImageUrl());
                    repository.delete(product);
                    log.info("✅ Produto ID {} e sua imagem correspondente foram excluídos.", id);

              return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

}
