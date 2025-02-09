package com.onlinemarketplace.marketplaceservice.repository;

import com.onlinemarketplace.marketplaceservice.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT b FROM Product b WHERE b.product_id = :productId")
    Optional<Product> findByProduct_id(Integer productId);

    @Query("SELECT b.stock_quantity FROM Product b where b.product_id = :productId")
    Optional<Integer> getStockQuantityByProduct_id(Integer productId);
}
