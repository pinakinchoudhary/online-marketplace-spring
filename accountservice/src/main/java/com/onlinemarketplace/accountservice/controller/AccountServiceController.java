package com.onlinemarketplace.accountservice.controller;

import com.onlinemarketplace.accountservice.model.AccountUser;
import com.onlinemarketplace.accountservice.repository.AccountUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
public class AccountServiceController {
    private final AccountUserRepository accountUserRepository;
    @Autowired
    public AccountServiceController(AccountUserRepository accountUserRepository) {
        this.accountUserRepository = accountUserRepository;
    }

    @PostMapping(value = "/users", consumes = "application/json")
    public ResponseEntity<AccountUser> createAccount(@RequestBody AccountUser accountUser) {
        AccountUser accountUserSaved = accountUserRepository.save(accountUser);
        System.out.println("Creating account user: " + accountUserSaved);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping(path = "/user/{id}")
    public ResponseEntity<AccountUser> getAccount(@PathVariable Long id) {
        System.out.println("Retrieving account user by id: " + id);
        Optional<AccountUser> accountUser = accountUserRepository.findById(id);
        System.out.println("Retrieved account user: " + accountUser.toString());
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
