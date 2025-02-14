package com.onlinemarketplace.marketplaceservice.controller;

import com.onlinemarketplace.marketplaceservice.model.*;
import com.onlinemarketplace.marketplaceservice.repository.OrderItemRepository;
import com.onlinemarketplace.marketplaceservice.repository.OrderRepository;
import com.onlinemarketplace.marketplaceservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = "api/v1")
public class MarketplaceServiceController {
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    private final RestClient restClient;
    private static final String baseURI = "http://host.docker.internal";
    private static String accountServiceEndpoint = ":8080/api/v1";
    private static String walletServiceEndpoint = ":8082/api/v1";

    public MarketplaceServiceController(ProductRepository productRepository, OrderRepository orderRepository, OrderItemRepository orderItemRepository, RestClient restClient) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.restClient = restClient;
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
        // Send request to AccountService GET /users/{user_id} and check
        User user;
        try {
            user = restClient.get()
                    .uri(baseURI + accountServiceEndpoint + "/users/" + orderRequestBody.getUser_id())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(User.class);
        } catch (RestClientException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Assuming a valid user_id
        int totalCost = 0;
        for (OrderItem orderItem : orderRequestBody.getItems()) {
            if (productRepository.getStockQuantityByProduct_id(orderItem.getProductId()).get() < orderItem.getQuantity()) {
                return new ResponseEntity<>("Stock not available!", HttpStatus.BAD_REQUEST);
            } else {
                totalCost += productRepository.findByProduct_id(orderItem.getProductId()).get().getProductId() * orderItem.getQuantity()
            }
        }
        //  If discount valid, deduct 10%
        if (user.getDiscount_valid()) {
            totalCost -= (int) (totalCost * 0.1);
        }

        //  Call wallet service to deduct the amount, if insufficient balance return 400.
        try {
            ResponseEntity<Void> deductBalance = restClient.put()
                    .uri(baseURI + walletServiceEndpoint + "/wallets/" + user.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"action\":\"debit\", \"balance\":" + totalCost + "}")
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // If debit succeeds
        for (OrderItem orderItem : orderRequestBody.getItems()) {
            Product product = productRepository.findByProduct_id(orderItem.getProductId()).get();
            product.setStockQuantity(product.getStockQuantity() - orderItem.getQuantity());
            productRepository.save(product);
        }
        //  Set discount_valid=false for {user_id} using PUT endpoint
        if (user.getDiscount_valid()) {
            try {
                user.setDiscount_valid(false);
                ResponseEntity<Void> updateDiscount = restClient.put()
                        .uri(baseURI + accountServiceEndpoint + "user/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(user.toString())
                        .retrieve()
                        .toBodilessEntity();
            } catch (RestClientException e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        // Create Order record
        Order order = new Order();
        order.setUserId(user.getId());
        order.setTotalPrice(totalCost);
        order.setStatus("PLACED");
        try {
            orderRepository.save(order);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        // prepare a string to return as payload
        String orderRecord =
    }

}
