package com.onlinemarketplace.accountservice.controller;

import com.onlinemarketplace.accountservice.model.User;
import com.onlinemarketplace.accountservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
public class AccountServiceController {
    private final UserRepository userRepository;
    @Autowired
    public AccountServiceController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping(value = "/users", consumes = "application/json")
    public ResponseEntity<User> createAccount(@RequestBody User user) {
        User userSaved = userRepository.save(user);
        System.out.println("Creating account user: " + userSaved);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping(path = "/user/{id}")
    public ResponseEntity<User> getAccount(@PathVariable Long id) {
        System.out.println("Retrieving account user by id: " + id);
        User user = userRepository.findByUserId(id);
        System.out.println("Retrieved account user: " + user.toString());
        return new ResponseEntity<>(user, HttpStatus.OK);
    }
}
