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

    /**
     * Retrieves the stock quantity of a product by its ID.
     *
     * @param product_id the ID of the product whose stock quantity is to be retrieved
     * @return an {@link Optional} containing the stock quantity if found, otherwise empty
     */
    @Query("SELECT p.stock_quantity FROM Product p WHERE p.id = :product_id")
    Optional<Integer> findStock_quantityByProduct_id(Integer product_id);

    /**
     * Decreases the stock quantity of a product by a specified amount.
     *
     * @param product_id the ID of the product whose stock quantity is to be decreased
     * @param quantity   the amount by which to decrease the stock quantity
     */
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stock_quantity = (p.stock_quantity - :quantity) WHERE p.id = :product_id")
    void decreaseStockQuantityByProduct_id(Integer product_id, int quantity);

    /**
     * Increases the stock quantity of a product by a specified amount.
     *
     * @param product_id the ID of the product whose stock quantity is to be increased
     * @param quantity   the amount by which to increase the stock quantity
     */
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stock_quantity = (p.stock_quantity + :quantity) WHERE p.id = :product_id")
    void increaseStockQuantityByProduct_id(Integer product_id, int quantity);

    /**
     * Retrieves the price of a product by its ID.
     *
     * @param product_id the ID of the product whose price is to be retrieved
     * @return an {@link Optional} containing the price if found, otherwise empty
     */
    @Query("SELECT p.price FROM Product p WHERE p.id = :product_id")
    Optional<Integer> findPriceById(Integer product_id);
}


