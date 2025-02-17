package com.onlinemarketplace.accountservice.controller;

import com.onlinemarketplace.accountservice.model.User;
import com.onlinemarketplace.accountservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AccountServiceController {
    private final UserRepository userRepository;
    private final RestClient restClient;
    private static final String baseURI = "http://host.docker.internal";
    private static final String marketplaceServiceEndpoint = ":8081";
    private static final String walletServiceEndpoint = ":8082";

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

    /**
     * Updates the discount_availed field for a user.
     *
     * @param id The user ID.
     * @return ResponseEntity indicating success or failure.
     */
    @PutMapping("/updateDiscount/{id}")
    public ResponseEntity<?> discount(@PathVariable("id") Integer id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, String.format("User not found with id %d", id)));
            try {
                userRepository.updateDiscountAvailedByIdById(user.getId(), true);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Discount update failed!", e);
            }
            return new ResponseEntity<>("Discount successfully updated!", HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }

    /**
     * Creates a new user account.
     *
     * @param user The user details.
     * @return ResponseEntity with created user details or an error message.
     */
    @PostMapping(value = "/users", consumes = "application/json")
    public ResponseEntity<?> createAccount(@RequestBody User user) {
        try {
            if (userRepository.findByEmail(user.getEmail()).isEmpty()) {
                try {
                    userRepository.save(user);
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User creation failed!", e);
                }
                return new ResponseEntity<>(user, HttpStatus.CREATED);
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists!");
            }
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }

    /**
     * Retrieves a user account by ID.
     *
     * @param userId The user ID.
     * @return ResponseEntity with user details or an error message.
     */
    @GetMapping(path = "/users/{userId}")
    public ResponseEntity<?> getAccount(@PathVariable Integer userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, String.format("User not found with id %d", userId)));
            return new ResponseEntity<>(user, HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }

    /**
     * Deletes a user account by ID and removes related data from external services.
     *
     * @param id The user ID.
     * @return ResponseEntity indicating success or failure.
     */
    @DeleteMapping(path= "/users/{id}")
    public ResponseEntity<?> deleteAccount(@PathVariable Integer id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("User not found with id %d", id)));
            try {
                userRepository.deleteById(id);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User deletion failed!", e);
            }

            // Remove user's orders from Marketplace
            String marketplaceResponse = restClient.delete()
                    .uri(baseURI + marketplaceServiceEndpoint + "/marketplace/users/" + id)
                    .exchange(((clientRequest, clientResponse) -> {
                        if (clientResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
                            return "No User order found in Marketplace!";
                        } else if (clientResponse.getStatusCode().is2xxSuccessful()) {
                            return "User order(s) successfully cancelled in Marketplace.";
                        } else {
                            throw new ResponseStatusException(clientResponse.getStatusCode(), "User order deletion failed!");
                        }
                    }));

            // Remove user's wallet data
            String walletResponse = restClient.delete()
                    .uri(baseURI + walletServiceEndpoint + "/wallets/" + id)
                    .exchange(((clientRequest, clientResponse) -> {
                        if (clientResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
                            return "No User wallet found in Marketplace!";
                        } else if (clientResponse.getStatusCode().is2xxSuccessful()) {
                            return "User wallet successfully deleted.";
                        } else {
                            throw new ResponseStatusException(clientResponse.getStatusCode(), "User wallet deletion failed!");
                        }
                    }));

            return new ResponseEntity<>("User deleted successfully.\n" + marketplaceResponse + "\n" + walletResponse, HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }

    /**
     * Deletes all user accounts and their related data from external services.
     *
     * @return ResponseEntity indicating success or failure.
     */
    @DeleteMapping(path = "/users")
    public ResponseEntity<String> deleteAllAccounts() {
        try {
            try {
                userRepository.deleteAll();
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User deletion failed!", e);
            }

            try {
                restClient.delete()
                        .uri(baseURI + marketplaceServiceEndpoint + "/marketplace")
                        .retrieve()
                        .toEntity(String.class);
            } catch (HttpClientErrorException e) {
                throw new ResponseStatusException(e.getStatusCode(), "Marketplace deletion unsuccessful!", e);
            }

            try {
                restClient.delete()
                        .uri(baseURI + walletServiceEndpoint + "/wallets")
                        .retrieve()
                        .toEntity(String.class);
            } catch (HttpClientErrorException e) {
                throw new ResponseStatusException(e.getStatusCode(), "Wallet deletion unsuccessful!", e);
            }

            return new ResponseEntity<>("All users deleted!", HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }
}
