package com.onlinemarketplace.marketplaceservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@Component
public class ProductInitializer implements CommandLineRunner {

    @Autowired
    private ProductService productService;

    @Autowired
    private ResourceLoader resourceLoader;

    /**
     * Loads product data from a CSV file when the application starts.
     *
     * @param args command line arguments passed to the application
     * @throws Exception if an error occurs while reading the CSV file or loading products
     */
    @Override
    public void run(String... args) throws Exception {
        Resource resource = resourceLoader.getResource("classpath:static/products.csv");

        try (InputStream inputStream = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            productService.loadProductsFromCSV(reader);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error reading CSV file", e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error while loading products", e);
        }
    }
}

