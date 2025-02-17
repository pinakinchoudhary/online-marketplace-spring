package com.onlinemarketplace.marketplaceservice.model;

/**
 * Represents a request body for wallet operations.
 * This class is used to encapsulate the action to be performed on the wallet
 * and the amount involved in that action.
 */
public class WalletRequestBody {

    /**
     * The action to be performed on the wallet.
     * This could be actions such as "deposit", "credit"
     */
    private String action;

    /**
     * The amount of money to be added or removed from the wallet.
     * This value should be a positive integer for deposits and a non-negative integer for withdrawals.
     */
    private Integer amount;

    public WalletRequestBody(String action, Integer amount) {
        this.action = action;
        this.amount = amount;
    }

    // Getters and Setters
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }
}
