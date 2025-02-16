package com.onlinemarketplace.marketplaceservice.controller;


import com.onlinemarketplace.marketplaceservice.model.*;
import com.onlinemarketplace.marketplaceservice.repository.OrderRepository;
import com.onlinemarketplace.marketplaceservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class MarketplaceServiceController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final RestClient restClient;
    private static final String baseURI = "http://localhost";
    private static final String accountServiceEndpoint = ":8080/users/";
    private static final String discountUpdateEndpoint = ":8080/updateDiscount/";
    private static final String walletServiceEndpoint = ":8082/wallets/";

    @Autowired
    public MarketplaceServiceController(OrderRepository orderRepository, ProductRepository productRepository, RestClient restClient) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.restClient = restClient;
    }


    @GetMapping("/products")
    public ResponseEntity<?> getAllProducts() {
        try {
            List<Product> productList = productRepository.findAll();
            return new ResponseEntity<>(productList, HttpStatus.OK);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while fetching Products!", e);
        }
    }

    @GetMapping("/products/{product_id}")
    public ResponseEntity<?> getProductById(@PathVariable Integer product_id) {
        try {
            Product product = productRepository.findById(product_id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found!"));
            return new ResponseEntity<>(product, HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }

    @PostMapping(value = "/orders", consumes = "application/json")
    public ResponseEntity<?> addOrder(@RequestBody Order order) {
        try {
            User user = restClient.get()
                    .uri(baseURI + accountServiceEndpoint + order.getUser_id())
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange(((clientRequest, clientResponse) -> {
                        if (clientResponse.getStatusCode().is2xxSuccessful()) {
                            return clientResponse.bodyTo(User.class);
                        } else if (clientResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
                            throw new ResponseStatusException(clientResponse.getStatusCode(), clientResponse.bodyTo(String.class));
                        } else {
                            throw new ResponseStatusException(clientResponse.getStatusCode(), "Error while fetching User!");
                        }
                    }));

            int totalCost = 0;
            for (OrderItem orderItem : order.getItems()) {
                int productStock = productRepository.findStock_quantityByProduct_id(orderItem.getProduct_id())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Error while fetching product stock!"));
                if (orderItem.getQuantity() > productStock) {
                    return new ResponseEntity<>("Out of Stock!", HttpStatus.BAD_REQUEST);
                }
                int productPrice = productRepository.findPriceById(orderItem.getProduct_id())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Error while fetching product price!"));
                totalCost += orderItem.getQuantity() * productPrice;
            }

            assert user != null;
            if (!user.getDiscount_availed()) {
                totalCost = (int) (totalCost * 0.9);
            }

            try {
                ResponseEntity<Void> walletServiceResponse = restClient.put()
                        .uri(baseURI + walletServiceEndpoint + order.getUser_id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new WalletRequestBody("debit", totalCost))
                        .retrieve()
                        .toBodilessEntity();
            } catch (RestClientResponseException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getResponseBodyAsString(), e);
            }

            try {
                ResponseEntity<Void> discountResponse = restClient.put()
                        .uri(baseURI + discountUpdateEndpoint + order.getUser_id())
                        .retrieve()
                        .toBodilessEntity();
            } catch (RestClientResponseException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getResponseBodyAsString(), e);
            }

            for (OrderItem orderItem : order.getItems()) {
                try {
                    productRepository.decreaseStockQuantityByProduct_id(orderItem.getProduct_id(), orderItem.getQuantity());
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while decrease stock quantity!", e);
                }
            }

            order.setTotal_price(totalCost);
            order.setStatus("PLACED");
            try {
                orderRepository.save(order);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while saving order!", e);
            }
            return new ResponseEntity<>(order, HttpStatus.CREATED);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }

    @GetMapping("/orders/{order_id}")
    public ResponseEntity<?> getOrderById(@PathVariable Integer order_id) {
        try {
            Order order = orderRepository.findById(order_id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found!"));
            return new ResponseEntity<>(order, HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }

    @GetMapping("/orders/users/{user_id}")
    public ResponseEntity<?> getOrdersByUserId(@PathVariable Integer user_id) {
        try {
            try {
                List<Order> orders = orderRepository.findAllByUserId(user_id);
                return new ResponseEntity<>(orders, HttpStatus.OK);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while fetching orders!", e);
            }
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }

    @DeleteMapping("/orders/{order_id}")
    public ResponseEntity<?> deleteOrderById(@PathVariable Integer order_id) {
        try {
            Order order = orderRepository.findById(order_id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found!"));

            if (order.getStatus().equals("PLACED")) {
                order.setStatus("CANCELLED");

                for (OrderItem orderItem : order.getItems()) {
                    try {
                        productRepository.increaseStockQuantityByProduct_id(orderItem.getProduct_id(), orderItem.getQuantity());
                    } catch (Exception e) {
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while increasing stock quantity!", e);
                    }
                }

                try {
                    ResponseEntity<Void> walletServiceResponse = restClient.put()
                            .uri(baseURI + walletServiceEndpoint + order.getUser_id())
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(new WalletRequestBody("credit", order.getTotal_price()))
                            .retrieve()
                            .toBodilessEntity();
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error while crediting from wallet!");
                }
                return new ResponseEntity<>(HttpStatus.OK);
            }
            return new ResponseEntity<>("Order Cancelled or Delivered!", HttpStatus.BAD_REQUEST);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }


    @PutMapping(value = "/orders/{order_id}", consumes = "application/json")
    public ResponseEntity<?> updateOrder(@PathVariable Integer order_id, @RequestBody Order order) {
        try {
            Order newOrder = orderRepository.findById(order_id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found!"));
            if (newOrder.getStatus().equals("PLACED")) {
                try {
                    orderRepository.updateStatusByOrder_id(order_id, "DELIVERED");
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while updating order status!", e);
                }
                return new ResponseEntity<>("Order delivered Successfully!", HttpStatus.OK);
            } else if (newOrder.getStatus().equals("DELIVERED")) {
                return new ResponseEntity<>("Order already delivered!", HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Order was CANCELLED!", HttpStatus.BAD_REQUEST);
            }
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }

    @DeleteMapping("/marketplace/users/{user_id}")
    public ResponseEntity<?> deleteMarketplaceById(@PathVariable Integer user_id) {
        try {
            List<Order> orders;
            try {
                orders = orderRepository.findAllByUserId(user_id);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while fetching orders!", e);
            }
            if (!orders.isEmpty()) {
                for (Order order : orders) {
                    try {
                        deleteOrderById(order.getOrder_id());
                    } catch (Exception e) {
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while deleting order!", e);
                    }
                }
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No orders exist for the given user!");
            }
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }

    @DeleteMapping("/marketplace")
    public ResponseEntity<?> deleteMarketplace() {
        try {
            List<Order> orders;
            try {
                orders = orderRepository.findAll();
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while fetching orders!", e);
            }
            for (Order order : orders) {
                try {
                    deleteOrderById(order.getOrder_id());
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while deleting orders!", e);
                }
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }
}





















