package com.onlinemarketplace.accountservice.model;

import jakarta.persistence.*;

@Entity
@Table(name="account_user")
public class User {
    /**
     * Id of the User
     * Provided in payload of HTTP request.
     */
    @Id
    private Integer id;
    private String name;
    /**
     * Every email should be unique.
     */
    @Column(unique=true)
    private String email;
    private Boolean discount_availed = false;


    // Getter and Setters
    @Override
    public String toString() {
        return '{' +
                "id:" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", discount_availed=" + discount_availed +
                '}';
    }

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
