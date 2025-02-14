package com.onlinemarketplace.marketplaceservice.model;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class Order {
    @Id
    @SequenceGenerator(
            name = "order_generator",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "order_generator"
    )
    @Column(name = "order_id")
    private Integer order_id;
    private Integer user_id;
    private Integer total_price;
    private String status;

    @OneToMany
    List<OrderItem> orderItems;

    public Integer getOrder_id() {
        return order_id;
    }

    public void setOrder_id(Integer id) {
        this.order_id = id;
    }

    public Integer getUser_id() {
        return user_id;
    }

    public void setUser_id(Integer user_id) {
        this.user_id = user_id;
    }

    public Integer getTotal_price() {
        return total_price;
    }

    public void setTotal_price(Integer total_price) {
        this.total_price = total_price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }
}
