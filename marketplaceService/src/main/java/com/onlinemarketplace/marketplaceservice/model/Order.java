package com.onlinemarketplace.marketplaceservice.model;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    /**
     * The unique identifier for the order.
     * This ID is generated automatically using a sequence generator.
     */
    @Id
    @SequenceGenerator(
            name = "order_generator",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "order_generator"
    )
    private Integer order_id;
    private Integer user_id;
    private Integer total_price;
    private String status;
    /**
     * The list of items associated with this order.
     * This establishes a one-to-many relationship with the {@link OrderItem} entity.
     * The cascade option allows for operations on the order eg. .save() to be propagated to its items.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items;


    // Getter and Setters
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

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
        items.forEach(item -> item.setOrder(this));
    }

    @Override
    public String toString() {
        return "Order{" +
                "order_id=" + order_id +
                ", user_id=" + user_id +
                ", total_price=" + total_price +
                ", status='" + status + '\'' +
                ", orderItems=" + items +
                '}';
    }
}
