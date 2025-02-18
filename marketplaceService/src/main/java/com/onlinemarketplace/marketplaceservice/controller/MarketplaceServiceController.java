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
    private static final String baseURI = "http://host.docker.internal";
    private static final String accountServiceEndpoint = ":8080/users/";
    private static final String discountUpdateEndpoint = ":8080/updateDiscount/";
    private static final String walletServiceEndpoint = ":8082/wallets/";

    /**
     * Constructor for dependency injection.
     * @param orderRepository Repository for handling Order entities.
     * @param productRepository Repository for handling Product entities.
     * @param restClient Rest client for making external API calls.
     */
    @Autowired
    public MarketplaceServiceController(OrderRepository orderRepository, ProductRepository productRepository, RestClient restClient) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.restClient = restClient;
    }

    /**
     * Retrieves all products.
     *
     * Handles GET requests to "/products".
     * Returns a list of products with a 200 (OK) status.
     * If an error occurs during retrieval, a 500 (INTERNAL SERVER ERROR) status is thrown.
     *
     * @return ResponseEntity containing the list of products.
     */
    @GetMapping("/products")
    public ResponseEntity<?> getAllProducts() {
        try {
            List<Product> productList = productRepository.findAll();
            return new ResponseEntity<>(productList, HttpStatus.OK);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while fetching Products!", e);
        }
    }

    /**
     * Retrieves a product by its ID.
     *
     * Handles GET requests to "/products/{product_id}".
     * Returns the product details with a 200 (OK) status if found,
     * or a 404 (NOT FOUND) status with an error message if not.
     *
     * @param product_id the unique identifier of the product.
     * @return ResponseEntity containing the product or an error message.
     */
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

    // check for invalid orderItem payload
    private boolean isValidOrderItem(final OrderItem orderItem) {
        if (orderItem.getQuantity() == null) {
            return false;
        } else if (orderItem.getProduct_id() == null) {
            return false;
        }
        return true;
    }
    // checks for valid payload
    private boolean isValidPayloadForPostMethod(final Order order) {
        if (order.getUser_id() == null) {
            return false;
        } else if (order.getItems() == null || order.getItems().isEmpty()) {
            return false;
        } for (OrderItem orderItem : order.getItems()) {
            if (!isValidOrderItem(orderItem)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Handles the HTTP POST request to add a new order.
     *
     * This method processes an incoming order request, validates the order items,
     * checks the user's eligibility for discounts, updates the user's wallet,
     * and decreases the stock quantity of the ordered products. If any step fails,
     * appropriate exceptions are thrown, and a relevant HTTP response is returned.
     *
     * @param order The order object containing user ID and order items to be processed.
     * @return A ResponseEntity containing the created order and HTTP status code.
     *         If an error occurs, it returns an error message and the corresponding HTTP status.
     */
    @PostMapping(value = "/orders", consumes = "application/json")
    public ResponseEntity<?> addOrder(@RequestBody Order order) {
        try {
            if (!isValidPayloadForPostMethod(order)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payload!");
            }
            User user = restClient.get()
                    .uri(baseURI + accountServiceEndpoint + order.getUser_id())
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange(((clientRequest, clientResponse) -> {
                        if (clientResponse.getStatusCode().is2xxSuccessful()) {
                            return clientResponse.bodyTo(User.class);
                        } else if (clientResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, clientResponse.bodyTo(String.class));
                        } else {
                            throw new ResponseStatusException(clientResponse.getStatusCode(), "Error while fetching User!");
                        }
                    }));

            int totalCost = 0;
            for (OrderItem orderItem : order.getItems()) {
                int productStock = productRepository.findStock_quantityByProduct_id(orderItem.getProduct_id())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Error while fetching product stock!"));
                if (orderItem.getQuantity() > productStock) {
                    String productName = productRepository.findById(orderItem.getProduct_id()).get().getName();
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, productName + " is out of stock!");
                } else if (orderItem.getQuantity() > 0) {
                    int productPrice = productRepository.findPriceById(orderItem.getProduct_id())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Error while fetching product price!"));
                    totalCost += orderItem.getQuantity() * productPrice;
                } else {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product quantity is less than or equal to zero!");
                }
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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error while saving order!", e);
            }
            return new ResponseEntity<>(order, HttpStatus.CREATED);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }

    /**
     * Retrieves an order by its ID.
     * This method fetches the order from the repository based on the provided order ID.
     *
     * @param order_id the ID of the order to be retrieved
     * @return a ResponseEntity containing the order and HTTP status
     */
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

    /**
     * Retrieves all orders associated with a specific user.
     * This method fetches the list of orders for the given user ID from the repository.
     *
     * @param user_id the ID of the user whose orders are to be retrieved
     * @return a ResponseEntity containing the list of orders and HTTP status
     */
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

    /**
     * Deletes an order by its ID.
     * This method cancels the order if it is in the "PLACED" state, increases the stock quantity
     * of the associated products, and credits the user's wallet with the order's total price.
     *
     * @param order_id the ID of the order to be deleted
     * @return a ResponseEntity indicating the result of the operation
     */
    @DeleteMapping("/orders/{order_id}")
    public ResponseEntity<?> deleteOrderById(@PathVariable Integer order_id) {
        try {
            Order order = orderRepository.findById(order_id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order not found!"));

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
                return new ResponseEntity<>(String.format("Order %d Cancelled.", order_id), HttpStatus.OK);
            }
            return new ResponseEntity<>("Order Cancelled or Delivered!", HttpStatus.BAD_REQUEST);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }

    private boolean isValidPayloadForPutMethod(final Order order) {
        if (order.getUser_id() == null) {
            return false;
        } else if (order.getStatus() == null) {
            return false;
        }
        return true;
    }

    /**
     * Updates the status of an existing order to "DELIVERED".
     * This method checks the current status of the order and updates it if the order is in the "PLACED" state.
     *
     * @param order_id the ID of the order to be updated
     * @param order    the order object containing the new status (not used in this implementation)
     * @return a ResponseEntity indicating the result of the operation
     */
    @PutMapping(value = "/orders/{order_id}", consumes = "application/json")
    public ResponseEntity<?> updateOrder(@PathVariable Integer order_id, @RequestBody Order order) {
        try {
            if (!isValidPayloadForPutMethod(order)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payload!");
            }
            Order newOrder = orderRepository.findById(order_id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order not found!"));
            if (!order.getStatus().equals("DELIVERED")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad request body!");
            }
            if (newOrder.getStatus().equals("PLACED")) {
                try {
                    orderRepository.updateStatusByOrder_id(order_id, "DELIVERED");
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while updating order status!", e);
                }
                return new ResponseEntity<>("Order delivered Successfully!", HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Order was CANCELLED or DELIVERED!", HttpStatus.BAD_REQUEST);
            }
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }

    /**
     * Deletes all orders associated with a specific user.
     * This method retrieves all orders for the given user ID and deletes each one.
     *
     * @param user_id the ID of the user whose orders are to be deleted
     * @return a ResponseEntity indicating the result of the operation
     */
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

    /**
     * Deletes all orders in the marketplace.
     * This method retrieves all orders from the repository and deletes each one.
     *
     * @return a ResponseEntity indicating the result of the operation
     */
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
            return new ResponseEntity<>("Marketplace deleted!", HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }
}





















