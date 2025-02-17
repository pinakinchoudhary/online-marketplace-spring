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
    /**
     * The order to which this order item belongs.
     * This establishes a many-to-one relationship with the {@link Order} entity.
     * The column "order_id" in database table will store foreign keys.
     * "order" represents the mappedBy field name in @OneToMany of {@link Order}
     */
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    private Integer product_id;
    private Integer quantity;

    // Implementing the getOrder() method may lead to infinite recursion during JSON serialization
    // as it references the parent Order object. So, omitted.

    public void setOrder(Order order) {
        this.order = order;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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
