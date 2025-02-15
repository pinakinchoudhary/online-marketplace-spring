package com.onlinemarketplace.marketplaceservice.model;


import jakarta.persistence.*;

@Entity
public class OrderItem {

    @Id
    @SequenceGenerator(
            name = "order_item_generator",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "order_item_generator"
    )
    private Integer id;
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    private Integer product_id;
    private Integer quantity;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    // remove to prevent infinite jsonification from response entity
//    public Order getOrder() {
//        return order;
//    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Integer getProduct_id() {
        return product_id;
    }

    public void setProduct_id(Integer product_id) {
        this.product_id = product_id;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
