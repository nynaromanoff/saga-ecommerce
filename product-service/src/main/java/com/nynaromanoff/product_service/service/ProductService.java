package com.nynaromanoff.product_service.service;


import com.nynaromanoff.product_service.dto.ProductRequest;
import com.nynaromanoff.product_service.dto.ProductResponse;
import com.nynaromanoff.product_service.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;


public interface ProductService {
    ProductResponse createProduct(ProductRequest productRequest, MultipartFile file);
    ProductResponse getProductBySku(String sku);
    Page<Product> listAll(Pageable pageable);
    Page<Product> searchProductByName(String name, Pageable pageable);
    void deleteProduct(UUID id);
}
