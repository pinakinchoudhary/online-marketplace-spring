package com.onlinemarketplace.accountservice.controller;

import com.onlinemarketplace.accountservice.model.User;
import com.onlinemarketplace.accountservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@RestController
@RequestMapping(path = "api/v1")
public class AccountServiceController {
    private final UserRepository userRepository;
    private final RestClient restClient;
    private static final String baseURI = "http://host.docker.internal";
    private static final String marketplaceServiceEndpoint = ":8081/api/v1";
    private static final String walletServiceEndpoint = ":8082/api/v1/";

    @Autowired
    public AccountServiceController(UserRepository userRepository, RestClient restClient) {
        this.userRepository = userRepository;
        this.restClient = restClient;
    }

    // Handle a PUT request for updating the discount_valid field
    @PutMapping(value = "/users", consumes = "application/json")
    public ResponseEntity<String> updateDiscount(@RequestBody User user) {
        if (userRepository.findByUserID(user.getUserID()).isPresent()) {
            return new ResponseEntity<>("No such User exists!", HttpStatus.BAD_REQUEST);
        }
        userRepository.save(user);
        return new ResponseEntity<>("Discount updated successfully!", HttpStatus.OK);
    }

    @PostMapping(value = "/users", consumes = "application/json")
    public ResponseEntity<String> createAccount(@RequestBody User user) {
        // Changed ResponseEntity<User> to <String> to be able to return email already exists error.
        if (userRepository.findByEmail(user.getEmail()).isEmpty()) {
            userRepository.save(user);
            return new ResponseEntity<>(user.toString() + " Added!", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Email already exists!", HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/users/{userId}")
    public ResponseEntity<String> getAccount(@PathVariable Integer userId) {
        Optional<User> user = userRepository.findByUserID(userId);
        if (user.isPresent()) {
            return new ResponseEntity<>(user.get().toString(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("User not found!", HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping(path= "/users/{userId}")
    public ResponseEntity<String> deleteAccount(@PathVariable Integer userId) {
        Optional<User> user = userRepository.findByUserID(userId);
        if (user.isPresent()) {
            userRepository.deleteByUserID(user.get().getUserID());
            //  DELETE /marketplace/users/{userId} to delete cancel and remove user's orders
            ResponseEntity<String> response = restClient.delete()
                    .uri(baseURI + marketplaceServiceEndpoint + "/marketplace/users/" + userId)
                    .retrieve()
                    .toEntity(String.class);
            if (response.getStatusCode() != HttpStatus.OK) {
                return new ResponseEntity<>("Marketplace deletion unsuccessful!", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            //  DELETE /wallet/{userId}
            response = restClient.delete()
                    .uri(baseURI + walletServiceEndpoint + "/wallets/" + userId)
                    .retrieve()
                    .toEntity(String.class);
            if (response.getStatusCode() != HttpStatus.OK) {
                return new ResponseEntity<>("Wallet deletion unsuccessful!", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            return new ResponseEntity<>(user.get().toString() + " Deleted!", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("No such User exists!", HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping(path = "/users")
    public ResponseEntity<String> deleteAllAccounts() {
        userRepository.deleteAll();
        //  DELETE /marketplace to reset all orders, product and discount_usage, etc.
        ResponseEntity<String> response = restClient.delete()
                .uri(baseURI + marketplaceServiceEndpoint + "/marketplace")
                .retrieve()
                .toEntity(String.class);
        if (response.getStatusCode() != HttpStatus.OK) {
            return new ResponseEntity<>("Marketplace deletion unsuccessful!", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        //  DELETE /wallets to remove all wallets
        response = restClient.delete()
                .uri(baseURI + walletServiceEndpoint + "/wallets")
                .retrieve()
                .toEntity(String.class);
        if (response.getStatusCode() != HttpStatus.OK) {
            return new ResponseEntity<>("Wallet deletion unsuccessful!", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>("All users deleted!", HttpStatus.OK);
    }
}
