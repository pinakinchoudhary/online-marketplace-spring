package com.onlinemarketplace.accountservice.model;

import jakarta.persistence.*;

@Entity
@Table(name="account_user")
public class User {
    @Id
    @SequenceGenerator(
            name="user_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "user_sequence"
    )
    private Long userID;
    // Note: This works because in Payload JSON, id field is not present, it's auto-generated. All other names in payload
    // must match the ones defined here!
    private String name;
    @Column(unique=true)
    private String email;
    private Boolean discount_valid = false;

    @Override
    public String toString() {
        return "AccountUser{" +
                "id=" + userID +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", discount_valid=" + discount_valid +
                '}';
    }

    public Long getUserID() {
        return userID;
    }

    public void setUserID(Long id) {
        this.userID = id;
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
