package com.onlinemarketplace.accountservice.controller;

import com.onlinemarketplace.accountservice.model.User;
import com.onlinemarketplace.accountservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping(path = "api/v1")
public class AccountServiceController {
    private final UserRepository userRepository;

    @Autowired
    public AccountServiceController(UserRepository userRepository) {
        this.userRepository = userRepository;
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
    public ResponseEntity<String> getAccount(@PathVariable Long userId) {
        Optional<User> user = userRepository.findByUserID(userId);
        if (user.isPresent()) {
            return new ResponseEntity<>(user.get().toString(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("User not found!", HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping(path="/users/{id}")
    public ResponseEntity<String> deleteAccount(@PathVariable Long id) {
        Optional<User> user = userRepository.findByUserID(id);
        if (user.isPresent()) {
            userRepository.deleteByUserID(user.get().getUserID());
            // TODO: DELETE /marketplace/users/{userId} to delete cancel and remove user's orders
            // TODO: DELETE /wallet/{userId}
            return new ResponseEntity<>(user.get().toString() + " Deleted!", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("No such User exists!", HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping(path = "/users")
    public ResponseEntity<String> deleteAllAccounts() {
        userRepository.deleteAll();
        // TODO: DELETE /marketplace to reset all orders, product and discount_usage, etc.
        // TODO: DELETE /wallet to remove all wallets
        return new ResponseEntity<>("All users deleted!", HttpStatus.OK);
    }
}
