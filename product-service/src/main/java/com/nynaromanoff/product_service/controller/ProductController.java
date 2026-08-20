package com.nynaromanoff.product_service.controller;

import com.nynaromanoff.product_service.dto.ProductCreatedEvent;
import com.nynaromanoff.product_service.dto.ProductRequest;
import com.nynaromanoff.product_service.dto.ProductResponse;
import com.nynaromanoff.product_service.model.Product;
import com.nynaromanoff.product_service.producer.ProductProducer;
import com.nynaromanoff.product_service.repository.ProductRepository;
import com.nynaromanoff.product_service.service.StorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;


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
    public ResponseEntity<Product> create(
            @RequestPart("product") ProductRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {

            if (repository.findBySku(request.getSku()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            String imageUrl = null;
            if (image != null && !image.isEmpty()) {
                imageUrl = storage.uploadFile(image);
            }

            Product product = Product.builder()
                    .sku(request.getSku().toUpperCase())
                    .name(request.getName())
                    .description(request.getDescription())
                    .price(request.getPrice())
                    .imageUrl(imageUrl)
                    .build();

            repository.save(product);

            ProductCreatedEvent event =  new ProductCreatedEvent(
                    product.getId(),
                    product.getSku(),
                    product.getName());
            productProducer.sendProductCreatedMessage(event);

            return ResponseEntity.status(HttpStatus.CREATED).body(product);
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

}
