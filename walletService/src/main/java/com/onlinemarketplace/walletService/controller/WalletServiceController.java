package com.onlinemarketplace.walletService.controller;

import com.onlinemarketplace.walletService.model.Wallet;
import com.onlinemarketplace.walletService.model.WalletRequestBody;
import com.onlinemarketplace.walletService.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping(path = "api/v1")
public class WalletServiceController {
    private final WalletRepository walletRepository;
    @Autowired
    public WalletServiceController(final WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @GetMapping(path = "wallets/{userId}")
    public ResponseEntity<String> getWallet(@PathVariable("userId") Integer userId) {
        Optional<Wallet> wallet = walletRepository.findByUserId(userId);
        if (wallet.isPresent()) {
            return new ResponseEntity<>(wallet.get().toString(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Wallet not found!", HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(value = "/wallets/{user_id}", consumes = "application/json")
    public ResponseEntity<String> updateWallet(@PathVariable("user_id") Integer userId, @RequestBody WalletRequestBody walletRequestBody) {
        Optional<Wallet> walletOptional = walletRepository.findByUserId(userId);
        Wallet wallet = walletOptional.orElseGet(Wallet::new);
        wallet.setUser_id(userId);
        if (walletRequestBody.getAction().equals("credit")) {
            wallet.setBalance(wallet.getBalance() + walletRequestBody.getAmount());
        } else if (walletRequestBody.getAction().equals("debit")) {
            if (wallet.getBalance() < walletRequestBody.getAmount()) {
                walletRepository.save(wallet);
                return new ResponseEntity<>("You don't have enough money!", HttpStatus.BAD_REQUEST);
            } else {
                wallet.setBalance(wallet.getBalance() - walletRequestBody.getAmount());
            }
        }
        walletRepository.save(wallet);
        return new ResponseEntity<>(wallet.toString(), HttpStatus.OK);
    }

    @DeleteMapping(path = "/wallets/{userId}")
    public ResponseEntity<String> deleteWallet(@PathVariable("userId") Integer userId) {
        Optional<Wallet> walletOptional = walletRepository.findByUserId(userId);
        if (walletOptional.isPresent()) {
            walletRepository.delete(walletOptional.get());
            return new ResponseEntity<>(walletOptional.get().toString(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Wallet not found!", HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping(path = "/wallets")
    public ResponseEntity<String> deleteWallets() {
        walletRepository.deleteAll();
        return new ResponseEntity<>("Wallets deleted!", HttpStatus.OK);
    }
}