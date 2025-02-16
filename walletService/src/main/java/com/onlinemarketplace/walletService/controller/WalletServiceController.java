package com.onlinemarketplace.walletService.controller;

import com.onlinemarketplace.walletService.model.Wallet;
import com.onlinemarketplace.walletService.model.WalletRequestBody;
import com.onlinemarketplace.walletService.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
public class WalletServiceController {
    private final WalletRepository walletRepository;
    private final RestClient restClient;
    private static final String baseURI = "http://localhost";
    private static final String accountServiceEndpoint = ":8080/users/";


    @Autowired
    public WalletServiceController(final WalletRepository walletRepository, final RestClient restClient) {
        this.walletRepository = walletRepository;
        this.restClient = restClient;
    }

    @GetMapping(path = "wallets/{user_id}")
    public ResponseEntity<?> getWallet(@PathVariable("user_id") Integer user_id) {
        try {
            Wallet wallet = walletRepository.findByUser_id(user_id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found!"));
            return new ResponseEntity<>(wallet, HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }

    @PutMapping(value = "/wallets/{user_id}", consumes = "application/json")
    public ResponseEntity<?> updateWallet(@PathVariable("user_id") Integer user_id, @RequestBody WalletRequestBody walletRequestBody) {
        try {
            try {
                ResponseEntity<Void> accountServiceResponse = restClient.get()
                        .uri(baseURI + accountServiceEndpoint + user_id)
                        .retrieve()
                        .toBodilessEntity();
            } catch (RestClientResponseException e) {
                throw new ResponseStatusException(e.getStatusCode(), e.getResponseBodyAsString(), e);
            }

            Optional<Wallet> walletOptional = walletRepository.findByUser_id(user_id);
            Wallet wallet = walletOptional.orElseGet(Wallet::new);
            wallet.setUser_id(user_id);
            if (walletRequestBody.getAction().equals("credit")) {
                wallet.setBalance(wallet.getBalance() + walletRequestBody.getAmount());
            } else if (walletRequestBody.getAction().equals("debit")) {
                if (wallet.getBalance() < walletRequestBody.getAmount()) {
                    try {
                        walletRepository.save(wallet);
                    } catch (Exception e) {
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while saving wallet", e);
                    }
                    return new ResponseEntity<>("Insufficient Balance!", HttpStatus.BAD_REQUEST);
                } else {
                    wallet.setBalance(wallet.getBalance() - walletRequestBody.getAmount());
                }
            }
            try {
                walletRepository.save(wallet);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while saving wallet!", e);
            }
            return new ResponseEntity<>(wallet, HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }

    @DeleteMapping(path = "/wallets/{user_id}")
    public ResponseEntity<?> deleteWallet(@PathVariable("user_id") Integer user_id) {
        try {
            try {
                walletRepository.deleteById(user_id);
            } catch (EmptyResultDataAccessException e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found!");
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while deleting wallet!", e);
            }
            return new ResponseEntity<>("Wallet has been deleted.", HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }

    @DeleteMapping(path = "/wallets")
    public ResponseEntity<?> deleteWallets() {
        try {
            try {
                walletRepository.deleteAll();
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while deleting wallets!", e);
            }
            return new ResponseEntity<>("All wallets deleted.", HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }
}