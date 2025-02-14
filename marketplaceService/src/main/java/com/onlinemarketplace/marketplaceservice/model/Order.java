package com.onlinemarketplace.marketplaceservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import java.util.List;

@Entity
public class Order {
    @Id
    private Integer order_id;
    private Integer user_id;
    private Integer total_price;
    private String status;
    @OneToMany
    private List<OrderItem> order_items;

    

    // Getter & Setters
    public List<OrderItem> getOrder_items() {return order_items;}

    public void setOrder_items(List<OrderItem> order_items) {this.order_items = order_items;}

    public Integer getOrderId() {
        return order_id;
    }

    public void setOrderId(Integer order_id) {
        this.order_id = order_id;
    }

    public Integer getUserId() {
        return user_id;
    }

    public void setUserId(Integer user_id) {
        this.user_id = user_id;
    }

    public Integer getTotalPrice() {
        return total_price;
    }

    public void setTotalPrice(Integer total_price) {
        this.total_price = total_price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
