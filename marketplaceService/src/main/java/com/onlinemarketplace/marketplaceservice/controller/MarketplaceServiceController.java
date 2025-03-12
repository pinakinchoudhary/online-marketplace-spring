package com.onlinemarketplace.marketplaceservice.controller;

import com.onlinemarketplace.marketplaceservice.model.*;
import com.onlinemarketplace.marketplaceservice.repository.OrderRepository;
import com.onlinemarketplace.marketplaceservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@RestController
public class MarketplaceServiceController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final RestClient restClient;
    private static final String accountServiceURI = "http://accountservice:8080";
    private static final String walletServiceURI = "http://walletservice:8082";
    private static final String accountServiceEndpoint = "/users/";
    private static final String discountUpdateEndpoint = "/updateDiscount/";
    private static final String discountResetEndpoint = "/resetDiscount/";
    private static final String walletServiceEndpoint = "/wallets/";
    
    // Per-product locks to handle concurrent inventory modifications
    private final ConcurrentHashMap<Integer, Lock> productLocks = new ConcurrentHashMap<>();
    // Per-order locks to handle concurrent order operations
    private final ConcurrentHashMap<Integer, Lock> orderLocks = new ConcurrentHashMap<>();
    // Lock for creating new orders (to synchronize order ID generation)
    private final Lock newOrderLock = new ReentrantLock();

    // Get or create a lock for a specific product
    private Lock getProductLock(Integer productId) {
        return productLocks.computeIfAbsent(productId, k -> new ReentrantLock());
    }
    
    // Get or create a lock for a specific order
    private Lock getOrderLock(Integer orderId) {
        return orderLocks.computeIfAbsent(orderId, k -> new ReentrantLock());
    }

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
     * Handles GET requests to "/products".
     * Returns a list of products with a 200 (OK) status.
     * If an error occurs during retrieval, a 500 (INTERNAL SERVER ERROR) status is thrown.
     *
     * @return ResponseEntity containing the list of products.
     */
    @GetMapping("/products")
    @Transactional(readOnly = true)
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
     * Handles GET requests to "/products/{product_id}".
     * Returns the product details with a 200 (OK) status if found,
     * or a 404 (NOT FOUND) status with an error message if not.
     *
     * @param product_id the unique identifier of the product.
     * @return ResponseEntity containing the product or an error message.
     */
    @GetMapping("/products/{product_id}")
    @Transactional(readOnly = true)
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
     * The method uses transactions with SERIALIZABLE isolation to prevent concurrent
     * modifications to the database, and includes logic to revert changes to external
     * services if the transaction fails.
     *
     * @param order The order object containing user ID and order items to be processed.
     * @return A ResponseEntity containing the created order and HTTP status code.
     *         If an error occurs, it returns an error message and the corresponding HTTP status.
     */
    @PostMapping(value = "/orders", consumes = "application/json")
    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
    @Retryable(value = {OptimisticLockingFailureException.class, DataAccessException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public ResponseEntity<?> addOrder(@RequestBody Order order) {
        // Keep track of service calls that need to be reverted in case of failure
        boolean walletUpdated = false;
        boolean discountUpdated = false;
        List<Integer> reservedProductIds = new ArrayList<>();

        try {
            if (!isValidPayloadForPostMethod(order)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payload!");
            }

            // Get user information
            User user = getUserInfo(order.getUser_id());

            // Acquire new order lock to ensure consistent order creation
            newOrderLock.lock();
            try {
                // Validate products and calculate total cost atomically
                int totalCost = validateAndCalculateTotalCost(order.getItems());

                // Apply discount if eligible
                assert user != null;
                if (!user.getDiscount_availed()) {
                    totalCost = (int) (totalCost * 0.9);
                }

                // Update user's wallet - this is an external service call
                try {
                    updateWallet(order.getUser_id(), "debit", totalCost);
                    walletUpdated = true;
                } catch (RestClientResponseException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getResponseBodyAsString(), e);
                }

                // Update user's discount status - another external service call
                try {
                    updateDiscount(order.getUser_id());
                    discountUpdated = true;
                } catch (RestClientResponseException e) {
                    // Revert wallet update if discount update fails
                    if (walletUpdated) {
                        try {
                            updateWallet(order.getUser_id(), "credit", totalCost);
                            walletUpdated = false;
                        } catch (Exception revertException) {
                            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                    "Failed to update discount and could not revert wallet update: " +
                                            revertException.getMessage(), revertException);
                        }
                    }
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getResponseBodyAsString(), e);
                }

                // Decrease product stock quantities atomically with proper locking
                try {
                    for (OrderItem orderItem : order.getItems()) {
                        Lock productLock = getProductLock(orderItem.getProduct_id());
                        
                        try {
                            if (!productLock.tryLock(3, TimeUnit.SECONDS)) {
                                throw new ResponseStatusException(HttpStatus.CONFLICT, 
                                        "Could not acquire lock for product " + orderItem.getProduct_id() + ", please try again later.");
                            }
                            
                            try {
                                productRepository.decreaseStockQuantityByProduct_id(orderItem.getProduct_id(), orderItem.getQuantity());
                                reservedProductIds.add(orderItem.getProduct_id());
                            } finally {
                                productLock.unlock();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Operation interrupted");
                        }
                    }
                } catch (Exception e) {
                    // Revert inventory changes
                    for (int i = 0; i < reservedProductIds.size(); i++) {
                        OrderItem item = order.getItems().get(i);
                        try {
                            Lock productLock = getProductLock(item.getProduct_id());
                            boolean locked = false;
                            try {
                                locked = productLock.tryLock(1, TimeUnit.SECONDS);
                                if (locked) {
                                    productRepository.increaseStockQuantityByProduct_id(item.getProduct_id(), item.getQuantity());
                                }
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            } finally {
                                if (locked) {
                                    productLock.unlock();
                                }
                            }
                        } catch (Exception ex) {
                            // Just log the error since we're already handling another exception
                            System.err.println("Failed to revert inventory change: " + ex.getMessage());
                        }
                    }
                    
                    // Revert external service calls if stock update fails
                    revertExternalServiceCalls(order.getUser_id(), totalCost, walletUpdated, discountUpdated);
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while decreasing stock quantity!", e);
                }

                // Save the order
                order.setTotal_price(totalCost);
                order.setStatus("PLACED");
                try {
                    orderRepository.save(order);
                } catch (Exception e) {
                    // Revert inventory changes
                    for (int i = 0; i < reservedProductIds.size(); i++) {
                        OrderItem item = order.getItems().get(i);
                        try {
                            Lock productLock = getProductLock(item.getProduct_id());
                            if (productLock.tryLock(1, TimeUnit.SECONDS)) {
                                try {
                                    productRepository.increaseStockQuantityByProduct_id(item.getProduct_id(), item.getQuantity());
                                } finally {
                                    productLock.unlock();
                                }
                            }
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        } catch (Exception ex) {
                            System.err.println("Failed to revert inventory change: " + ex.getMessage());
                        }
                    }
                    
                    // Revert external service calls and stock changes if order save fails
                    revertExternalServiceCalls(order.getUser_id(), totalCost, walletUpdated, discountUpdated);
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error while saving order!", e);
                }
            } finally {
                newOrderLock.unlock();
            }

            return new ResponseEntity<>(order, HttpStatus.CREATED);
        } catch (ResponseStatusException e) {
            // No need to revert anything here as it should be handled in the specific catch blocks
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            // Ensure we revert any external service calls for unexpected exceptions
            revertExternalServiceCalls(order.getUser_id(), order.getTotal_price(), walletUpdated, discountUpdated);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to get user information from the account service.
     *
     * @param userId the ID of the user
     * @return User object with user information
     * @throws ResponseStatusException if the user is not found or an error occurs
     */
    private User getUserInfo(Integer userId) {
        return restClient.get()
                .uri(accountServiceURI + accountServiceEndpoint + userId)
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
    }

    /**
     * Validates order items and calculates the total cost.
     * Uses per-product locking to prevent concurrent updates to product inventory
     *
     * @param orderItems list of order items
     * @return the total cost of the order
     * @throws ResponseStatusException if validation fails
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    protected int validateAndCalculateTotalCost(List<OrderItem> orderItems) {
        int totalCost = 0;
        for (OrderItem orderItem : orderItems) {
            Lock lock = getProductLock(orderItem.getProduct_id());
            try {
                // Try to acquire lock with timeout to prevent deadlocks
                if (!lock.tryLock(3, TimeUnit.SECONDS)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, 
                            "Could not acquire lock for product " + orderItem.getProduct_id() + ", please try again later.");
                }
                
                try {
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
                } finally {
                    lock.unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Operation interrupted");
            }
        }
        return totalCost;
    }

    /**
     * Updates a user's wallet balance with improved error handling and retries.
     *
     * @param userId the ID of the user
     * @param action "credit" or "debit"
     * @param amount the amount to credit or debit
     * @throws RestClientResponseException if the wallet service returns an error
     */
    @Retryable(value = {RestClientException.class}, maxAttempts = 3, 
               backoff = @Backoff(delay = 1000, multiplier = 2))
    private void updateWallet(Integer userId, String action, int amount) {
        ResponseEntity<Void> walletServiceResponse = restClient.put()
                .uri(walletServiceURI + walletServiceEndpoint + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new WalletRequestBody(action, amount))
                .retrieve()
                .toBodilessEntity();
    }
    
    @Recover
    private void walletUpdateFallback(RestClientException e, Integer userId, String action, int amount) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, 
                "Wallet service is unavailable after multiple attempts: " + e.getMessage(), e);
    }

    /**
     * Updates a user's discount status with improved error handling and retries.
     *
     * @param userId the ID of the user
     * @throws RestClientResponseException if the account service returns an error
     */
    @Retryable(value = {RestClientException.class}, maxAttempts = 3, 
               backoff = @Backoff(delay = 1000, multiplier = 2))
    private void updateDiscount(Integer userId) {
        ResponseEntity<Void> discountResponse = restClient.put()
                .uri(accountServiceURI + discountUpdateEndpoint + userId)
                .retrieve()
                .toBodilessEntity();
    }
    
    @Recover
    private void discountUpdateFallback(RestClientException e, Integer userId) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, 
                "Account service is unavailable after multiple attempts: " + e.getMessage(), e);
    }
    
    /**
     * Resets a user's discount status - needed for proper error recovery
     * 
     * @param userId the ID of the user
     */
    @Retryable(value = {RestClientException.class}, maxAttempts = 3, 
               backoff = @Backoff(delay = 1000, multiplier = 2))
    private void resetDiscount(Integer userId) {
        try {
            ResponseEntity<Void> discountResponse = restClient.put()
                    .uri(accountServiceURI + discountResetEndpoint + userId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            // Just log the error since this is a recovery operation
            System.err.println("Failed to reset discount for user " + userId + ": " + e.getMessage());
        }
    }

    /**
     * Reverts external service calls in case of failure with improved error handling.
     *
     * @param userId the ID of the user
     * @param amount the amount to revert
     * @param walletUpdated whether the wallet was updated
     * @param discountUpdated whether the discount was updated
     */
    private void revertExternalServiceCalls(Integer userId, int amount, boolean walletUpdated, boolean discountUpdated) {
        // If wallet was debited, credit it back
        if (walletUpdated) {
            try {
                updateWallet(userId, "credit", amount);
            } catch (Exception e) {
                // Log the error but continue with other reversions
                System.err.println("Failed to revert wallet update: " + e.getMessage());
            }
        }

        // If discount was updated, reset it
        if (discountUpdated) {
            try {
                resetDiscount(userId);
            } catch (Exception e) {
                // Log the error but continue
                System.err.println("Failed to reset discount update: " + e.getMessage());
            }
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
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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
    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
    @Retryable(value = {OptimisticLockingFailureException.class, DataAccessException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public ResponseEntity<?> deleteOrderById(@PathVariable Integer order_id) {
        Lock orderLock = getOrderLock(order_id);
        boolean lockAcquired = false;

        try {
            // Try to acquire lock with timeout to prevent deadlocks
            if (!orderLock.tryLock(3, TimeUnit.SECONDS)) {
                return new ResponseEntity<>("Order is currently being modified by another request, please try again.", 
                        HttpStatus.CONFLICT);
            }
            
            lockAcquired = true;
            
            Order order = orderRepository.findById(order_id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order not found!"));

            if (order.getStatus().equals("PLACED")) {
                // Update order status first to prevent concurrent cancellations
                order.setStatus("CANCELLING");
                orderRepository.save(order);

                boolean stockIncreased = false;
                boolean walletCredited = false;
                List<OrderItem> processedItems = new ArrayList<>();

                try {
                    // Increase stock quantities with proper locking
                    for (OrderItem orderItem : order.getItems()) {
                        Lock productLock = getProductLock(orderItem.getProduct_id());
                        try {
                            if (!productLock.tryLock(3, TimeUnit.SECONDS)) {
                                throw new ResponseStatusException(HttpStatus.CONFLICT, 
                                        "Could not update inventory, please try again later.");
                            }
                            
                            try {
                                productRepository.increaseStockQuantityByProduct_id(orderItem.getProduct_id(), orderItem.getQuantity());
                                processedItems.add(orderItem);
                            } finally {
                                productLock.unlock();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Operation interrupted");
                        }
                    }
                    stockIncreased = true;

                    // Credit wallet
                    try {
                        updateWallet(order.getUser_id(), "credit", order.getTotal_price());
                        walletCredited = true;
                    } catch (Exception e) {
                        // If wallet credit fails, revert stock changes
                        if (stockIncreased) {
                            for (OrderItem orderItem : processedItems) {
                                try {
                                    Lock productLock = getProductLock(orderItem.getProduct_id());
                                    if (productLock.tryLock(1, TimeUnit.SECONDS)) {
                                        try {
                                            productRepository.decreaseStockQuantityByProduct_id(orderItem.getProduct_id(), orderItem.getQuantity());
                                        } finally {
                                            productLock.unlock();
                                        }
                                    }
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    System.err.println("Operation interrupted while reverting inventory change");
                                } catch (Exception ex) {
                                    // Log error but continue with reversion
                                    System.err.println("Failed to revert inventory change: " + ex.getMessage());
                                }
                            }
                        }
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error while crediting wallet: " + e.getMessage());
                    }

                    // Set final status to CANCELLED
                    order.setStatus("CANCELLED");
                    orderRepository.save(order);

                    // Clean up the lock to prevent memory leaks
                    orderLocks.remove(order_id);
                    
                    return new ResponseEntity<>(String.format("Order %d Cancelled.", order_id), HttpStatus.OK);
                } catch (Exception e) {
                    // Revert any changes if an exception occurred
                    if (walletCredited) {
                        try {
                            updateWallet(order.getUser_id(), "debit", order.getTotal_price());
                        } catch (Exception revertException) {
                            // Log error but continue
                            System.err.println("Failed to revert wallet credit: " + revertException.getMessage());
                        }
                    }

                    // Revert order status
                    order.setStatus("PLACED");
                    orderRepository.save(order);

                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while cancelling order: " + e.getMessage(), e);
                }
            }
            return new ResponseEntity<>("Order Cancelled or Delivered!", HttpStatus.BAD_REQUEST);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ResponseEntity<>("Operation interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            if (lockAcquired) {
                orderLock.unlock();
            }
        }
    }

    private boolean isValidPayloadForPutMethod(final Order order) {
        if (order.getOrder_id() == null) {
            return false;
        } else if (order.getStatus() == null) {
            return false;
        }
        return true;
    }

    /**
     * Updates the status of an existing order to "DELIVERED" with improved concurrency control.
     *
     * @param order_id the ID of the order to be updated
     * @param order    the order object containing the new status
     * @return a ResponseEntity indicating the result of the operation
     */
    @PutMapping(value = "/orders/{order_id}", consumes = "application/json")
    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
    @Retryable(value = {OptimisticLockingFailureException.class, DataAccessException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public ResponseEntity<?> updateOrder(@PathVariable Integer order_id, @RequestBody Order order) {
        Lock orderLock = getOrderLock(order_id);
        boolean lockAcquired = false;
        
        try {
            // Try to acquire lock with timeout to prevent deadlocks
            if (!orderLock.tryLock(3, TimeUnit.SECONDS)) {
                return new ResponseEntity<>("Order is currently being modified by another request, please try again.", 
                        HttpStatus.CONFLICT);
            }
            
            lockAcquired = true;
            
            if (!isValidPayloadForPutMethod(order)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payload!");
            }

            Order existingOrder = orderRepository.findById(order_id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order not found!"));

            if (!order.getStatus().equals("DELIVERED")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad request body!");
            }

            if (existingOrder.getStatus().equals("PLACED")) {
                try {
                    // Use optimistic locking by first updating to an intermediate state
                    existingOrder.setStatus("DELIVERING");
                    orderRepository.save(existingOrder);

                    // Then update to the final state
                    existingOrder.setStatus("DELIVERED");
                    orderRepository.save(existingOrder);
                    
                    // Clean up the lock to prevent memory leaks
                    orderLocks.remove(order_id);

                    return new ResponseEntity<>("Order delivered Successfully!", HttpStatus.OK);
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while updating order status!", e);
                }
            } else {
                return new ResponseEntity<>("Order was CANCELLED or DELIVERED!", HttpStatus.BAD_REQUEST);
            }
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ResponseEntity<>("Operation interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            if (lockAcquired) {
                orderLock.unlock();
            }
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
    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
    public ResponseEntity<?> deleteMarketplaceById(@PathVariable Integer user_id) {
        try {
            List<Order> orders;
            try {
                orders = orderRepository.findAllByUserId(user_id);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while fetching orders!", e);
            }

            if (!orders.isEmpty()) {
                List<String> failedOrderIds = new ArrayList<>();

                for (Order order : orders) {
                    try {
                        // Use the existing method which has transaction handling
                        ResponseEntity<?> response = deleteOrderById(order.getOrder_id());
                        if (response.getStatusCode().isError()) {
                            failedOrderIds.add(order.getOrder_id().toString());
                        }
                    } catch (Exception e) {
                        failedOrderIds.add(order.getOrder_id().toString());
                    }
                }

                if (!failedOrderIds.isEmpty()) {
                    return new ResponseEntity<>("Failed to delete orders: " + String.join(", ", failedOrderIds),
                            HttpStatus.PARTIAL_CONTENT);
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

            List<String> failedOrderIds = new ArrayList<>();

            for (Order order : orders) {
                try {
                    // Use the existing method which has transaction handling
                    ResponseEntity<?> response = deleteOrderById(order.getOrder_id());
                    if (response.getStatusCode().isError()) {
                        failedOrderIds.add(order.getOrder_id().toString());
                    }
                } catch (Exception e) {
                    failedOrderIds.add(order.getOrder_id().toString());
                }
            }

            if (!failedOrderIds.isEmpty()) {
                return new ResponseEntity<>("Failed to delete orders: " + String.join(", ", failedOrderIds),
                        HttpStatus.PARTIAL_CONTENT);
            }

            return new ResponseEntity<>("Marketplace deleted!", HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }
}















