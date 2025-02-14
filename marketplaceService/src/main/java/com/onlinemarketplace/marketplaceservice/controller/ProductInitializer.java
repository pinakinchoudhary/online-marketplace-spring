package com.onlinemarketplace.marketplaceservice.controller;

import com.onlinemarketplace.marketplaceservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

@Component
public class ProductInitializer implements CommandLineRunner {
    @Autowired
    private ProductService productService;

    @Autowired
    private ResourceLoader resourceLoader;  // Inject ResourceLoader

    @Override
    public void run(String... args) throws Exception {
        // Load the CSV file from classpath (inside resources/static/)
        Resource resource = resourceLoader.getResource("classpath:static/products.csv");

        try (InputStream inputStream = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            productService.loadProductsFromCSV(reader);
        } catch (Exception e) {
            System.err.println("Error loading CSV file: " + e.getMessage());
        }




    }
}
