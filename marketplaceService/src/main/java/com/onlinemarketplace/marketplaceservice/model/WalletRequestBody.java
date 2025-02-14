package com.onlinemarketplace.marketplaceservice.model;

public class WalletRequestBody {
    private String action;
    private Integer amount;

    public WalletRequestBody(String action, Integer amount) {
        this.action = action;
        this.amount = amount;
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
