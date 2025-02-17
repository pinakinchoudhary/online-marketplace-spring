package com.onlinemarketplace.walletService.model;

/**
 * Represents the request body for wallet operations,
 * containing the action (credit or debit) and the amount
 * to be processed. This class is used to encapsulate
 * the data required for updating a user's wallet.
 */
public class WalletRequestBody {
    private String action;
    private Integer amount;

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
