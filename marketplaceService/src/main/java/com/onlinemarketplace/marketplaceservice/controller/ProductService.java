package com.onlinemarketplace.marketplaceservice.controller;


import com.onlinemarketplace.marketplaceservice.model.Product;
import com.onlinemarketplace.marketplaceservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;
    public void loadProductsFromCSV(BufferedReader br) {
        List<Product> productList = new ArrayList<>();
        String line;
        try {
            // Skip header line
            br.readLine();

            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length == 5) {  // Ensure correct column count
                    Product product = new Product();
                    product.setId(Integer.parseInt(data[0].trim()));
                    product.setName(data[1].trim());
                    product.setDescription(data[2].trim());
                    product.setPrice(Integer.parseInt(data[3].trim()));
                    product.setStock_quantity(Integer.parseInt(data[4].trim()));

                    productList.add(product);
                }
            }
            productRepository.saveAll(productList);
            System.out.println("Products loaded successfully!");
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
        }
    }
}

