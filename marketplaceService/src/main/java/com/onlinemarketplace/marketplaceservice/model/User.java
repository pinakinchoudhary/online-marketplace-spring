package com.onlinemarketplace.marketplaceservice.model;

import jakarta.persistence.*;



public class User {

    private Integer id;
    private String name;
    private String email;
    private Boolean discount_availed = false;

    // Getter and Setters
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

    public Boolean getDiscount_availed() {
        return discount_availed;
    }

    public void setDiscount_availed(Boolean discount_availed) {
        this.discount_availed = discount_availed;
    }
}
