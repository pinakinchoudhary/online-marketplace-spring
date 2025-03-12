package com.onlinemarketplace.accountservice.controller;

import com.onlinemarketplace.accountservice.model.User;
import com.onlinemarketplace.accountservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@RestController
public class AccountServiceController {
    private final UserRepository userRepository;
    private final RestClient restClient;
    private static final String marketplaceServiceURI = "http://marketplaceservice:8081";
    private static final String walletServiceURI = "http://walletservice:8082";
    
    // Global lock for user operations
    private final ReadWriteLock userOperationsLock = new ReentrantReadWriteLock();

    /**
     * Constructor for AccountServiceController.
     *
     * @param userRepository The user repository.
     * @param restClient     The REST client for making external service calls.
     */
    @Autowired
    public AccountServiceController(UserRepository userRepository, RestClient restClient) {
        this.userRepository = userRepository;
        this.restClient = restClient;
    }

    private boolean isValidPayloadForPostMethod(final User user) {
        if (user.getId() == null) {
            return false;
        } else if (user.getName() == null) {
            return false;
        } else if (user.getEmail() == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Updates the discount_availed field for a user.
     * Uses optimistic locking with retries to handle concurrent updates.
     *
     * @param id The user ID.
     * @return ResponseEntity indicating success or failure.
     */
    @PutMapping("/updateDiscount/{id}")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Retryable(value = {OptimisticLockingFailureException.class, DataAccessException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 500))
    public ResponseEntity<?> discount(@PathVariable("id") Integer id) {
        try {
            userOperationsLock.writeLock().lock();
            try {
                User user = userRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, String.format("User not found with id %d", id)));
                
                // Check if discount is already availed to make operation idempotent
                if (user.getDiscount_availed()) {
                    return new ResponseEntity<>("Discount already availed", HttpStatus.OK);
                }
                
                try {
                    userRepository.updateDiscountAvailedByIdById(user.getId(), true);
                } catch (OptimisticLockingFailureException e) {
                    // Let @Retryable handle this
                    throw e;
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Discount update failed!", e);
                }
                return new ResponseEntity<>("Discount successfully updated!", HttpStatus.OK);
            } finally {
                userOperationsLock.writeLock().unlock();
            }
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }

    /**
     * Creates a new user account.
     * Uses transaction isolation to prevent dirty reads and lost updates.
     *
     * @param user The user details.
     * @return ResponseEntity with created user details or an error message.
     */
    @PostMapping(value = "/users", consumes = "application/json")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ResponseEntity<?> createAccount(@RequestBody User user) {
        try {
            userOperationsLock.writeLock().lock();
            try {
                if (!isValidPayloadForPostMethod(user)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payload, all fields must be non-null.");
                }
                
                if (userRepository.findByEmail(user.getEmail()).isEmpty()) {
                    try {
                        // Set default value to ensure consistency
                        user.setDiscount_availed(false);
                        userRepository.save(user);
                    } catch (Exception e) {
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User creation failed!", e);
                    }
                    return new ResponseEntity<>(user, HttpStatus.CREATED);
                } else {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists!");
                }
            } finally {
                userOperationsLock.writeLock().unlock();
            }
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }

    /**
     * Retrieves a user account by ID.
     * Uses READ_COMMITTED isolation to allow concurrent reads.
     *
     * @param userId The user ID.
     * @return ResponseEntity with user details or an error message.
     */
    @GetMapping(path = "/users/{userId}")
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public ResponseEntity<?> getAccount(@PathVariable Integer userId) {
        try {
            userOperationsLock.readLock().lock();
            try {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, String.format("User not found with id %d", userId)));
                return new ResponseEntity<>(user, HttpStatus.OK);
            } finally {
                userOperationsLock.readLock().unlock();
            }
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }

    /**
     * Deletes a user account by ID and removes related data from external services.
     * Uses SERIALIZABLE isolation to prevent concurrent modifications.
     *
     * @param id The user ID.
     * @return ResponseEntity indicating success or failure.
     */
    @DeleteMapping(path= "/users/{id}")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Retryable(value = {DataAccessException.class},
            maxAttempts = 2, backoff = @Backoff(delay = 500))
    public ResponseEntity<?> deleteAccount(@PathVariable Integer id) {
        try {
            userOperationsLock.writeLock().lock();
            try {
                User user = userRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                                String.format("User not found with id %d", id)));
                
                // Track success of each operation
                boolean userDeleted = false;
                boolean marketplaceDeleted = false;
                boolean walletDeleted = false;
                String marketplaceResponse = "";
                String walletResponse = "";
                
                try {
                    userRepository.deleteById(id);
                    userDeleted = true;
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User deletion failed!", e);
                }

                // Remove user's orders from Marketplace with error handling
                try {
                    marketplaceResponse = restClient.delete()
                            .uri(marketplaceServiceURI + "/marketplace/users/" + id)
                            .exchange(((clientRequest, clientResponse) -> {
                                if (clientResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
                                    return "No User order found in Marketplace!";
                                } else if (clientResponse.getStatusCode().is2xxSuccessful()) {
                                    return "User order(s) successfully cancelled in Marketplace.";
                                } else {
                                    throw new ResponseStatusException(clientResponse.getStatusCode(), 
                                            "User order deletion failed!");
                                }
                            }));
                    marketplaceDeleted = true;
                } catch (Exception e) {
                    marketplaceResponse = "Failed to delete marketplace data: " + e.getMessage();
                }

                // Remove user's wallet data with error handling
                try {
                    walletResponse = restClient.delete()
                            .uri(walletServiceURI + "/wallets/" + id)
                            .exchange(((clientRequest, clientResponse) -> {
                                if (clientResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
                                    return "No User wallet found in Marketplace!";
                                } else if (clientResponse.getStatusCode().is2xxSuccessful()) {
                                    return "User wallet successfully deleted.";
                                } else {
                                    throw new ResponseStatusException(clientResponse.getStatusCode(), 
                                            "User wallet deletion failed!");
                                }
                            }));
                    walletDeleted = true;
                } catch (Exception e) {
                    walletResponse = "Failed to delete wallet data: " + e.getMessage();
                }

                String resultMessage = "User deleted successfully.";
                if (userDeleted && marketplaceDeleted && walletDeleted) {
                    resultMessage += "\n" + marketplaceResponse + "\n" + walletResponse;
                    return new ResponseEntity<>(resultMessage, HttpStatus.OK);
                } else {
                    resultMessage += "\n" + marketplaceResponse + "\n" + walletResponse;
                    return new ResponseEntity<>(resultMessage, HttpStatus.PARTIAL_CONTENT);
                }
            } finally {
                userOperationsLock.writeLock().unlock();
            }
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }

    /**
     * Deletes all user accounts and their related data from external services.
     * Uses SERIALIZABLE isolation to prevent concurrent modifications.
     *
     * @return ResponseEntity indicating success or failure.
     */
    @DeleteMapping(path = "/users")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ResponseEntity<String> deleteAllAccounts() {
        try {
            userOperationsLock.writeLock().lock();
            try {
                boolean usersDeleted = false;
                boolean marketplaceDeleted = false;
                boolean walletsDeleted = false;
                StringBuilder resultMessage = new StringBuilder();

                try {
                    userRepository.deleteAll();
                    usersDeleted = true;
                    resultMessage.append("All users deleted!\n");
                } catch (Exception e) {
                    resultMessage.append("User deletion failed: ").append(e.getMessage()).append("\n");
                }

                try {
                    restClient.delete()
                            .uri(marketplaceServiceURI + "/marketplace")
                            .retrieve()
                            .toEntity(String.class);
                    marketplaceDeleted = true;
                    resultMessage.append("All marketplace data deleted!\n");
                } catch (HttpClientErrorException e) {
                    resultMessage.append("Marketplace deletion failed: ").append(e.getMessage()).append("\n");
                } catch (Exception e) {
                    resultMessage.append("Marketplace deletion error: ").append(e.getMessage()).append("\n");
                }

                try {
                    restClient.delete()
                            .uri(walletServiceURI + "/wallets")
                            .retrieve()
                            .toEntity(String.class);
                    walletsDeleted = true;
                    resultMessage.append("All wallets deleted!\n");
                } catch (HttpClientErrorException e) {
                    resultMessage.append("Wallet deletion failed: ").append(e.getMessage()).append("\n");
                } catch (Exception e) {
                    resultMessage.append("Wallet deletion error: ").append(e.getMessage()).append("\n");
                }

                HttpStatus status = (usersDeleted && marketplaceDeleted && walletsDeleted) ? 
                        HttpStatus.OK : HttpStatus.PARTIAL_CONTENT;
                return new ResponseEntity<>(resultMessage.toString(), status);
            } finally {
                userOperationsLock.writeLock().unlock();
            }
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }
    
    /**
     * Resets a user's discount status to false.
     * Used for reverting discount status changes in failure scenarios.
     * 
     * @param userId The user ID
     * @return ResponseEntity indicating success or failure
     */
    @PutMapping("/resetDiscount/{userId}")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Retryable(value = {OptimisticLockingFailureException.class, DataAccessException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 500))
    public ResponseEntity<?> resetDiscount(@PathVariable Integer userId) {
        try {
            userOperationsLock.writeLock().lock();
            try {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, String.format("User not found with id %d", userId)));
                
                try {
                    userRepository.updateDiscountAvailedByIdById(user.getId(), false);
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                            "Discount reset failed!", e);
                }
                return new ResponseEntity<>("Discount reset successfully!", HttpStatus.OK);
            } finally {
                userOperationsLock.writeLock().unlock();
            }
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }
}
