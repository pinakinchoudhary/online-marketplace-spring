package com.onlinemarketplace.accountservice.model;

import jakarta.persistence.*;

@Entity
public class AccountUser {
    @Id
    @SequenceGenerator(
            name="user_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "user_sequence"
    )
    private Long id;
    private String name;
    @Column(unique=true)
    private String email;
    private Boolean discount_valid = false;

    @Override
    public String toString() {
        return "AccountUser{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", discount_valid=" + discount_valid +
                '}';
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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
