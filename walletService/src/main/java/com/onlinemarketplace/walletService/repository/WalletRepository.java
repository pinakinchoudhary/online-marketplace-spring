package com.onlinemarketplace.walletService.repository;

import com.onlinemarketplace.walletService.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Integer> {

    /**
     * Retrieves a Wallet associated with a specific user ID.
     *
     * @param user_id the ID of the user whose wallet is to be retrieved.
     * @return an Optional containing the Wallet if found, or empty if not.
     */
    @Query("SELECT w FROM Wallet w WHERE w.user_id = :user_id")
    Optional<Wallet> findByUser_id(Integer user_id);
}
