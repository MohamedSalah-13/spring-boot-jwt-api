package com.demo.productapi.product;

import com.demo.productapi.error.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    public Page<ProductResponse> search(String query, Pageable pageable) {
        Page<Product> page = StringUtils.hasText(query)
                ? products.findByNameContainingIgnoreCase(query.trim(), pageable)
                : products.findAll(pageable);
        return page.map(ProductResponse::from);
    }

    public ProductResponse get(Long id) {
        return ProductResponse.from(find(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product(request.name(), request.description(), request.price(), request.stock());
        return ProductResponse.from(products.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = find(id);
        product.update(request.name(), request.description(), request.price(), request.stock());
        return ProductResponse.from(products.saveAndFlush(product));
    }

    @Transactional
    public void delete(Long id) {
        products.delete(find(id));
    }

    private Product find(Long id) {
        return products.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + id + " was not found"));
    }
}
