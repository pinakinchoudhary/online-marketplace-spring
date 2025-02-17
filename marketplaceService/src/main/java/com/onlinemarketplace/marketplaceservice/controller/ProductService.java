package com.onlinemarketplace.marketplaceservice.controller;

import com.onlinemarketplace.marketplaceservice.model.Product;
import com.onlinemarketplace.marketplaceservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    /**
     * Loads products from a CSV file and saves them to the database.
     *
     * @param br the BufferedReader to read the CSV file
     * @throws ResponseStatusException if there is an error reading the CSV file or if the data is invalid
     */
    public void loadProductsFromCSV(BufferedReader br) {
        List<Product> productList = new ArrayList<>();
        String line;
        try {
            // Skip header line
            br.readLine();

            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length == 5) {  // Ensure correct column count
                    try {
                        Product product = new Product();
                        product.setId(Integer.parseInt(data[0].trim()));
                        product.setName(data[1].trim());
                        product.setDescription(data[2].trim());
                        product.setPrice(Integer.parseInt(data[3].trim()));
                        product.setStock_quantity(Integer.parseInt(data[4].trim()));

                        productList.add(product);
                    } catch (NumberFormatException e) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid number format in CSV file: " + e.getMessage());
                    }
                } else {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect column count in CSV file");
                }
            }
            productRepository.saveAll(productList);
            System.out.println("Products loaded successfully!");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error reading CSV file: " + e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred: " + e.getMessage());
        }
    }
}
