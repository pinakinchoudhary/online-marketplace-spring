package com.onlinemarketplace.walletService.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Represents a user's wallet in the online marketplace.
 * Contains the user's ID and the current balance.
 */
@Entity
public class Wallet {
    @Id
    private Integer user_id;
    private Integer balance = 0;

    public Integer getUser_id() {
        return user_id;
    }

    public void setUser_id(Integer user_id) {
        this.user_id = user_id;
    }

    public Integer getBalance() {
        return balance;
    }

    public void setBalance(Integer balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return "Wallet{" +
                "user_id=" + user_id +
                ", balance=" + balance +
                '}';
    }
}
