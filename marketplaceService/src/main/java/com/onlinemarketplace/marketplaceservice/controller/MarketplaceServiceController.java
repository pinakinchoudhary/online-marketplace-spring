package com.onlinemarketplace.marketplaceservice.controller;

import com.onlinemarketplace.marketplaceservice.model.Order;
import com.onlinemarketplace.marketplaceservice.model.OrderItem;
import com.onlinemarketplace.marketplaceservice.model.OrderRequestBody;
import com.onlinemarketplace.marketplaceservice.model.Product;
import com.onlinemarketplace.marketplaceservice.repository.OrderItemRepository;
import com.onlinemarketplace.marketplaceservice.repository.OrderRepository;
import com.onlinemarketplace.marketplaceservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = "api/v1")
public class MarketplaceServiceController {
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public MarketplaceServiceController(ProductRepository productRepository, OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProducts() {
        return new ResponseEntity<>(productRepository.findAll(), HttpStatus.OK);
    }

    @GetMapping("products/{productId}")
    public ResponseEntity<String> getProductById(@PathVariable Integer productId) {
        Optional<Product> product = productRepository.findByProduct_id(productId);
        if (product.isPresent()) {
            return new ResponseEntity<>(product.get().toString(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Product not found!", HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping(value = "/orders", consumes = "application/json")
    public ResponseEntity<String> addOrder(@RequestBody OrderRequestBody orderRequestBody) {
        // TODO: Send request to AccountService GET /users/{user_id} and check
        // Assuming a valid user_id
        int totalCost = 0;
        for (OrderItem orderItem : orderRequestBody.getItems()) {
            if (productRepository.getStockQuantityByProduct_id(orderItem.getProductId()).get() < orderItem.getQuantity()) {
                return new ResponseEntity<>("Stock not available!", HttpStatus.BAD_REQUEST);
            } else {
                totalCost += productRepository.findByProduct_id(orderItem.getProductId()).get().getProductId() * orderItem.getQuantity()
            }
        }
        // TODO: If discount valid, deduct 10%

        // TODO: Call wallet service to deduct the amount, if insuffiecient balance return 400.

        // If debit succeeds
        for (OrderItem orderItem : orderRequestBody.getItems()) {
            Product product = productRepository.findByProduct_id(orderItem.getProductId()).get();
            product.setStockQuantity(product.getStockQuantity() - orderItem.getQuantity());
            productRepository.save(product);
        }
        // TODO: Set discount_valid=false for {user_id} using PUT endpoint

        // TODO further
    }

}
