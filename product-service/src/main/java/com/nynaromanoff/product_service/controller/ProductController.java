package com.nynaromanoff.product_service.controller;

import com.nynaromanoff.product_service.dto.ProductCreatedEvent;
import com.nynaromanoff.product_service.dto.ProductRequest;
import com.nynaromanoff.product_service.dto.ProductResponse;
import com.nynaromanoff.product_service.model.Product;
import com.nynaromanoff.product_service.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
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
            ProductResponse response = service.createProduct(request, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        }catch (IllegalArgumentException e) {
            log.warn("⚠️ Validação de negócio: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        catch (Exception e) {
            log.error("❌ Falha crítica ao processar cadastro de produto e upload na AWS", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{sku}")
    public ResponseEntity<ProductResponse> getProductBySku(@PathVariable String sku) {
        log.info("🔍 [ProductAPI] Buscando produto pelo SKU: {}", sku);
       try {
           ProductResponse response = service.getProductBySku(sku);
           return ResponseEntity.ok(response);
       }catch (IllegalArgumentException e) {
           return ResponseEntity.notFound().build();
       }
    }

    @GetMapping
    public ResponseEntity<Page<Product>> listAll(Pageable pageable) {
        return ResponseEntity.ok(service.listAll(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<Product>> getProductByName(@RequestParam("name") String name,
                                                         Pageable pageable) {
        return ResponseEntity.ok(service.searchProductByName(name, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("Iniciando processo de exclusão do produto ID: {}", id);

        try {
            service.deleteProduct(id);
            return ResponseEntity.noContent().build();
        }catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

}
