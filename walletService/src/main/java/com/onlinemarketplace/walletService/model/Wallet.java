package com.onlinemarketplace.walletService.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Wallet {
    @Id
    private Integer user_id;
    private Integer balance = 0;

    @Override
    public String toString() {
        return "{" +
                "user_id=" + user_id +
                ", balance=" + balance +
                '}';
    }

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
}
