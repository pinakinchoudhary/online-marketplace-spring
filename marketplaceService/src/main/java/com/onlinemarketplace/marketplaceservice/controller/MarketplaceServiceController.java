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

import java.util.List;
import java.util.Optional;

@RestController
public class MarketplaceServiceController {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final RestClient restClient;
    private static final String baseURI = "http://localhost";
    private static final String accountServiceEndpoint = ":8080/users/";
    private static final String discountUpdateEndpoint = ":8080/updateDiscount/";
    private static final String walletServiceEndpoint = ":8082/wallets/";

    @Autowired
    public MarketplaceServiceController(OrderRepository orderRepository, OrderItemRepository orderItemRepository, ProductRepository productRepository, RestClient restClient) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.restClient = restClient;
    }


    @GetMapping("/products")
    public ResponseEntity<?> getAllProducts() {
        List<Product> productList = productRepository.findAll();

        return new ResponseEntity<>(productList, HttpStatus.OK);
    }

    @GetMapping("/products/{product_id}")
    public ResponseEntity<?> getProductById(@PathVariable Integer product_id) {
        Optional<Product> productOptional = productRepository.findById(product_id);
        if (productOptional.isPresent()) {
            return new ResponseEntity<>(productOptional.get(), HttpStatus.OK);
        }
        return new ResponseEntity<>("Product not found!", HttpStatus.NOT_FOUND);
    }

    @PostMapping(value = "/orders", consumes = "application/json")
    public ResponseEntity<?> addOrder(@RequestBody Order order) {
        ResponseEntity<User> accountServiceResponse;
        try {
            accountServiceResponse = restClient.get()
                    .uri(baseURI + accountServiceEndpoint + order.getUser_id())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(User.class);
        } catch (RestClientException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        int totalCost = 0;
        for (OrderItem orderItem : order.getItems()) {
            if (orderItem.getQuantity() > productRepository.findStock_quantityByProduct_id(orderItem.getProduct_id()).get()) {

                return new ResponseEntity<>("Out of Stock!", HttpStatus.BAD_REQUEST);
            }
            totalCost += orderItem.getQuantity() * productRepository.findPriceById(orderItem.getProduct_id()).get();
        }

        if (!accountServiceResponse.getBody().getDiscount_availed()) {

            totalCost = (int)(totalCost * 0.9);

        }

        try {
            ResponseEntity<Void> walletServiceResponse = restClient.put()
                    .uri(baseURI + walletServiceEndpoint + order.getUser_id())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new WalletRequestBody("debit", totalCost))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        try {
            ResponseEntity<Void> discountResponse = restClient.put()
                    .uri(baseURI + discountUpdateEndpoint + order.getUser_id())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        for (OrderItem orderItem : order.getItems()) {
            productRepository.decreaseStockQuantityByProduct_id(orderItem.getProduct_id(), orderItem.getQuantity());
        }
        order.setTotal_price(totalCost);
        order.setStatus("PLACED");
        try {
            orderRepository.save(order);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<Order>(order, HttpStatus.CREATED);
    }

    @GetMapping("/orders/{order_id}")
    public ResponseEntity<?> getOrderById(@PathVariable Integer order_id) {
        Optional<Order> order = orderRepository.findById(order_id);
        if (order.isPresent()) {
            return new ResponseEntity<>(order.get(), HttpStatus.OK);
        }
        return new ResponseEntity<>("Order not found!", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/orders/users/{user_id}")
    public ResponseEntity<?> getOrdersByUserId(@PathVariable Integer user_id) {
        List<Order> orders = orderRepository.findAllByUserId(user_id);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @DeleteMapping("/orders/{order_id}")
    public ResponseEntity<?> deleteOrderById(@PathVariable Integer order_id) {
        Optional<Order> order = orderRepository.findById(order_id);
        if (order.isPresent()) {
            if (order.get().getStatus().equals("PLACED")) {
                order.get().setStatus("CANCELLED");

                for (OrderItem orderItem : order.get().getItems()) {
                    productRepository.increaseStockQuantityByProduct_id(orderItem.getProduct_id(), orderItem.getQuantity());
                }

                try {
                    ResponseEntity<Void> walletServiceResponse = restClient.put()
                            .uri(baseURI + walletServiceEndpoint + order.get().getUser_id())
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(new WalletRequestBody("credit", order.get().getTotal_price()))
                            .retrieve()
                            .toBodilessEntity();
                } catch (RestClientException e) {
                    return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
                }

                return new ResponseEntity<>(HttpStatus.OK);
            }
            return new ResponseEntity<>("Order Cancelled or Delivered!", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>("Order not found!", HttpStatus.BAD_REQUEST);
    }


    @PutMapping(value = "/orders/{order_id}", consumes = "application/json")
    public ResponseEntity<?> updateOrder(@PathVariable Integer order_id, @RequestBody Order order) {
        Optional<Order> orderOptional = orderRepository.findById(order_id);
        if (orderOptional.isPresent() && orderOptional.get().getStatus().equals("PLACED")) {
            orderRepository.updateStatusByOrder_id(order_id, "DELIVERED");
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @DeleteMapping("/marketplace/users/{user_id}")
    public ResponseEntity<?> deleteMarketplaceById(@PathVariable Integer user_id) {
        List<Order> orders = orderRepository.findAllByUserId(user_id);
        if (!orders.isEmpty()) {
            for (Order order : orders) {
//                ResponseEntity<Void> marketplaceResponse = restClient.delete()
//                        .uri(baseURI + "/orders/users/" + order.getUser_id())
//                        .retrieve()
//                        .toBodilessEntity();
//                if (marketplaceResponse.getStatusCode() != HttpStatus.OK) {
//                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
//                }
                deleteOrderById(order.getOrder_id());
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Order not found!", HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/marketplace")
    public ResponseEntity<?> deleteMarketplace() {
        List<Order> orders = orderRepository.findAll();
        for (Order order : orders) {
//            ResponseEntity<Void> marketplaceResponse = restClient.delete()
//                    .uri(baseURI + "/orders/users/" + order.getUser_id())
//                    .retrieve()
//                    .toBodilessEntity();
//            if (marketplaceResponse.getStatusCode() != HttpStatus.OK) {
//                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
//            }
            deleteOrderById(order.getOrder_id());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
}





















