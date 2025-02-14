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
    @PutMapping("/updateDiscount/{id}")
    public ResponseEntity<?> discount(@PathVariable("id") Integer id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            userRepository.updateDiscountValidById(user.get().getId(), true);
            return new ResponseEntity<>("Discount successfully updated!", HttpStatus.OK);
        }
        return new ResponseEntity<>("User not found!", HttpStatus.NOT_FOUND);
    }

    @PostMapping(value = "/users", consumes = "application/json")
    public ResponseEntity<?> createAccount(@RequestBody User user) {
        if (userRepository.findByEmail(user.getEmail()).isEmpty()) {
            userRepository.save(user);
            return new ResponseEntity<>(user, HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Email already exists!", HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(path = "/users/{userId}")
    public ResponseEntity<?> getAccount(@PathVariable Integer userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent()) {
            return new ResponseEntity<>(user.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("User not found!", HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping(path= "/users/{id}")
    public ResponseEntity<?> deleteAccount(@PathVariable Integer id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            userRepository.deleteById(id);
            //  DELETE /marketplace/users/{userId} to delete cancel and remove user's orders
            ResponseEntity<String> response = restClient.delete()
                    .uri(baseURI + marketplaceServiceEndpoint + "/marketplace/users/" + id)
                    .retrieve()
                    .toEntity(String.class);
            if (response.getStatusCode() != HttpStatus.OK) {
                return new ResponseEntity<>("Marketplace deletion unsuccessful!", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            //  DELETE /wallet/{userId}
            response = restClient.delete()
                    .uri(baseURI + walletServiceEndpoint + "/wallets/" + id)
                    .retrieve()
                    .toEntity(String.class);
            if (response.getStatusCode() != HttpStatus.OK) {
                return new ResponseEntity<>("Wallet deletion unsuccessful!", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return new ResponseEntity<>(user.get() + "Wallet Deleted!", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("User not found!", HttpStatus.NOT_FOUND);
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
