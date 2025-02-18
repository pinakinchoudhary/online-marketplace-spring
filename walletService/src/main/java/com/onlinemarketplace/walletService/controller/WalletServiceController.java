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

/**
 * Endpoints:
 * - GET /wallets/{user_id}: Retrieve a user's wallet.
 * - PUT /wallets/{user_id}: Update a user's wallet (credit/debit).
 * - DELETE /wallets/{user_id}: Delete a user's wallet.
 * - DELETE /wallets: Delete all wallets.
 */
@RestController
public class WalletServiceController {
    private final WalletRepository walletRepository;
    private final RestClient restClient;
    private static final String baseURI = "http://host.docker.internal";
    private static final String accountServiceEndpoint = ":8080/users/";


    @Autowired
    public WalletServiceController(final WalletRepository walletRepository, final RestClient restClient) {
        this.walletRepository = walletRepository;
        this.restClient = restClient;
    }

    /**
     * Retrieves the wallet associated with the specified user ID.
     *
     * @param user_id the ID of the user whose wallet is to be retrieved
     * @return ResponseEntity containing the wallet details if found,
     *         or an error message if the wallet does not exist
     */
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

    boolean isValidPayloadForPutMethod(final WalletRequestBody walletRequestBody) {
        if (walletRequestBody.getAmount() == null) {
            return false;
        } else if (walletRequestBody.getAction() == null) {
            return false;
        }
        return true;
    }

    /**
     * Updates the wallet for the specified user ID based on the provided action (credit or debit).
     *
     * @param user_id          the ID of the user whose wallet is to be updated
     * @param walletRequestBody the request body containing the action and amount to be processed
     * @return ResponseEntity containing the updated wallet if successful,
     *         or an error message if the operation fails (e.g., insufficient balance or user not found)
     */
    @PutMapping(value = "/wallets/{user_id}", consumes = "application/json")
    public ResponseEntity<?> updateWallet(@PathVariable("user_id") Integer user_id, @RequestBody WalletRequestBody walletRequestBody) {
        try {
            if (!isValidPayloadForPutMethod(walletRequestBody)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payload!");
            }
            try {
                ResponseEntity<Void> accountServiceResponse = restClient.get()
                        .uri(baseURI + accountServiceEndpoint + user_id)
                        .retrieve()
                        .toBodilessEntity();
            } catch (RestClientResponseException e) {
                throw new ResponseStatusException(e.getStatusCode(), e.getResponseBodyAsString(), e);
            }
            if (walletRequestBody.getAmount() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero!");
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
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid action!");
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

    /**
     * Deletes the wallet associated with the specified user ID.
     *
     * @param user_id the ID of the user whose wallet is to be deleted
     * @return ResponseEntity confirming deletion if successful,
     *         or an error message if the wallet does not exist or an error occurs
     */
    @DeleteMapping(path = "/wallets/{user_id}")
    public ResponseEntity<?> deleteWallet(@PathVariable("user_id") Integer user_id) {
        try {
            Wallet wallet = walletRepository.findByUser_id(user_id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found!"));
            try {
                walletRepository.deleteById(user_id);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while deleting wallet!", e);
            }
            return new ResponseEntity<>("Wallet has been deleted.", HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
    }

    /**
     * Deletes all wallets in the system.
     *
     * @return ResponseEntity confirming deletion of all wallets if successful,
     *         or an error message if an error occurs during the deletion process
     */
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