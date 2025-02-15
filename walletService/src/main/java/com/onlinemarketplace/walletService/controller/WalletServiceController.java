package com.onlinemarketplace.walletService.controller;

import com.onlinemarketplace.walletService.model.Wallet;
import com.onlinemarketplace.walletService.model.WalletRequestBody;
import com.onlinemarketplace.walletService.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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
        Optional<Wallet> wallet = walletRepository.findByUser_id(user_id);
        if (wallet.isPresent()) {
            return new ResponseEntity<>(wallet.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Wallet not found!", HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(value = "/wallets/{user_id}", consumes = "application/json")
    public ResponseEntity<?> updateWallet(@PathVariable("user_id") Integer user_id, @RequestBody WalletRequestBody walletRequestBody) {
        try {
            ResponseEntity<Void> accountServiceResponse = restClient.get()
                    .uri(baseURI + accountServiceEndpoint + user_id)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        Optional<Wallet> walletOptional = walletRepository.findByUser_id(user_id);
        Wallet wallet = walletOptional.orElseGet(Wallet::new);
        wallet.setUser_id(user_id);
        if (walletRequestBody.getAction().equals("credit")) {
            wallet.setBalance(wallet.getBalance() + walletRequestBody.getAmount());
        } else if (walletRequestBody.getAction().equals("debit")) {
            if (wallet.getBalance() < walletRequestBody.getAmount()) {
                walletRepository.save(wallet);
                return new ResponseEntity<>("Insufficient Balance!", HttpStatus.BAD_REQUEST);
            } else {
                wallet.setBalance(wallet.getBalance() - walletRequestBody.getAmount());
            }
        }
        walletRepository.save(wallet);
        return new ResponseEntity<>(wallet, HttpStatus.OK);
    }

    @DeleteMapping(path = "/wallets/{user_id}")
    public ResponseEntity<?> deleteWallet(@PathVariable("user_id") Integer user_id) {
        Optional<Wallet> walletOptional = walletRepository.findByUser_id(user_id);
        if (walletOptional.isPresent()) {
            walletRepository.deleteById(user_id);
            return new ResponseEntity<>("Wallet has been deleted!", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Wallet not found!", HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping(path = "/wallets")
    public ResponseEntity<?> deleteWallets() {
        walletRepository.deleteAll();
        return new ResponseEntity<>("All wallets deleted!", HttpStatus.OK);
    }
}