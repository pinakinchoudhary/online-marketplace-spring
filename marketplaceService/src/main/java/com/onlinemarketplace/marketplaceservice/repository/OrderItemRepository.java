package com.onlinemarketplace.marketplaceservice.repository;

import com.onlinemarketplace.marketplaceservice.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

}
