package com.onlinemarketplace.marketplaceservice.controller;

import com.onlinemarketplace.marketplaceservice.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;

public class ProductInitializer implements CommandLineRunner {
    private final ProductRepository productRepository;
    public ProductInitializer(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // TODO: Logic to initialize Product Entity through CSV
    }
}
