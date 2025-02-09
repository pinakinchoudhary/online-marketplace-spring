package com.onlinemarketplace.marketplaceservice.model;

import jakarta.persistence.*;

@Entity
public class Product {
    @Id
    private Integer product_id;
    private String name;
    private String description;
    private Integer price;
    private Integer stock_quantity;

    @Override
    public String toString() {
        return "Product{" +
                "productId=" + product_id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", stock_quantity=" + stock_quantity +
                '}';
    }

    public Integer getProductId() {
        return product_id;
    }

    public void setProductId(Integer productId) {
        this.product_id = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public Integer getStockQuantity() {
        return stock_quantity;
    }

    public void setStockQuantity(Integer stock_quantity) {
        this.stock_quantity = stock_quantity;
    }
}
