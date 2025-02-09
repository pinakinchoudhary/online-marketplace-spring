package com.onlinemarketplace.walletService.model;

public class WalletRequestBody {
    private String action;
    private Integer amount;

    @Override
    public String toString() {
        return "WalletRequestBody{" +
                "action='" + action + '\'' +
                ", amount=" + amount +
                '}';
    }

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
