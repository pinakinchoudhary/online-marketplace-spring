package com.onlinemarketplace.walletService.repository;

import com.onlinemarketplace.walletService.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Integer> {

    @Query("SELECT w FROM Wallet w WHERE w.user_id = :user_id")
    Optional<Wallet> findByUser_id(Integer user_id);
}
