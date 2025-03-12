package com.onlinemarketplace.walletService.controller;

import com.onlinemarketplace.walletService.model.Wallet;
import com.onlinemarketplace.walletService.model.WalletRequestBody;
import com.onlinemarketplace.walletService.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
    private static final String accountServiceURI = "http://accountservice:8080";
    private static final String accountServiceEndpoint = "/users/";
    
    // Cache of user-specific locks to prevent concurrent operations on the same wallet
    private final ReadWriteLock globalWalletLock = new ReentrantReadWriteLock();

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
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public ResponseEntity<?> getWallet(@PathVariable("user_id") Integer user_id) {
        try {
            // Use read lock for concurrent reads
            globalWalletLock.readLock().lock();
            try {
                Wallet wallet = walletRepository.findByUser_id(user_id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found!"));
                return new ResponseEntity<>(wallet, HttpStatus.OK);
            } finally {
                globalWalletLock.readLock().unlock();
            }
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
     * Uses optimistic locking with retries to handle concurrent updates.
     *
     * @param user_id          the ID of the user whose wallet is to be updated
     * @param walletRequestBody the request body containing the action and amount to be processed
     * @return ResponseEntity containing the updated wallet if successful,
     *         or an error message if the operation fails
     */
    @PutMapping(value = "/wallets/{user_id}", consumes = "application/json")
    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
    @Retryable(value = {OptimisticLockingFailureException.class, DataAccessException.class},
            maxAttempts = 3, backoff = @Backoff(delay = 500))
    public ResponseEntity<?> updateWallet(@PathVariable("user_id") Integer user_id, @RequestBody WalletRequestBody walletRequestBody) {
        // Use write lock to ensure exclusive access during wallet update
        globalWalletLock.writeLock().lock();
        try {
            if (!isValidPayloadForPutMethod(walletRequestBody)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payload!");
            }
            
            // Verify user exists
            try {
                ResponseEntity<Void> accountServiceResponse = restClient.get()
                        .uri(accountServiceURI + accountServiceEndpoint + user_id)
                        .retrieve()
                        .toBodilessEntity();
            } catch (RestClientResponseException e) {
                throw new ResponseStatusException(e.getStatusCode(), e.getResponseBodyAsString(), e);
            }
            
            if (walletRequestBody.getAmount() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero!");
            }

            Optional<Wallet> walletOptional = walletRepository.findByUser_id(user_id);
            Wallet wallet = walletOptional.orElseGet(() -> {
                Wallet newWallet = new Wallet();
                newWallet.setUser_id(user_id);
                newWallet.setBalance(0);
                return newWallet;
            });

            if (walletRequestBody.getAction().equals("credit")) {
                wallet.setBalance(wallet.getBalance() + walletRequestBody.getAmount());
            } else if (walletRequestBody.getAction().equals("debit")) {
                if (wallet.getBalance() < walletRequestBody.getAmount()) {
                    if (!walletOptional.isPresent()) {
                        // Save new wallet even if debit fails to maintain consistent state
                        try {
                            walletRepository.save(wallet);
                        } catch (Exception e) {
                            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while saving wallet", e);
                        }
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
            } catch (OptimisticLockingFailureException e) {
                // Let @Retryable handle this
                throw e;
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while saving wallet!", e);
            }
            
            return new ResponseEntity<>(wallet, HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        } finally {
            globalWalletLock.writeLock().unlock();
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
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ResponseEntity<?> deleteWallet(@PathVariable("user_id") Integer user_id) {
        globalWalletLock.writeLock().lock();
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
        } finally {
            globalWalletLock.writeLock().unlock();
        }
    }

    /**
     * Deletes all wallets in the system.
     *
     * @return ResponseEntity confirming deletion of all wallets if successful,
     *         or an error message if an error occurs during the deletion process
     */
    @DeleteMapping(path = "/wallets")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ResponseEntity<?> deleteWallets() {
        globalWalletLock.writeLock().lock();
        try {
            try {
                walletRepository.deleteAll();
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error while deleting wallets!", e);
            }
            return new ResponseEntity<>("All wallets deleted.", HttpStatus.OK);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        } finally {
            globalWalletLock.writeLock().unlock();
        }
    }
}
