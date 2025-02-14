package com.onlinemarketplace.marketplaceservice.repository;

import com.onlinemarketplace.marketplaceservice.model.Product;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    @Query("SELECT p.stock_quantity FROM Product p WHERE p.id = :product_id")
    Optional<Integer> findStock_quantityByProduct_id(Integer product_id);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stock_quantity = (p.stock_quantity - :quantity) WHERE p.id = :product_id")
    void decreaseStockQuantityByProduct_id(Integer product_id, int quantity);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stock_quantity = (p.stock_quantity + :quantity) WHERE p.id = :product_id")
    void increaseStockQuantityByProduct_id(Integer product_id, int quantity);


    @Query("SELECT p.price FROM Product p WHERE p.id = :product_id")
    Optional<Integer> findPriceById(Integer product_id);
}


