package com.onlinemarketplace.walletService.repository;

import com.onlinemarketplace.walletService.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    @Query("SELECT b FROM Wallet b WHERE b.user_id = :userID")
    Optional<Wallet> findByUserId(Integer userID);
}
