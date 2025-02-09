package com.onlinemarketplace.marketplaceservice.repository;

import com.onlinemarketplace.marketplaceservice.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

}
