package com.onlinemarketplace.marketplaceservice.model;

public class User {
    private Integer id;
    private String name;
    private String email;
    private Boolean discount_valid = false;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getDiscount_valid() {
        return discount_valid;
    }

    public void setDiscount_valid(Boolean discount_valid) {
        this.discount_valid = discount_valid;
    }
}