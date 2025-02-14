package com.onlinemarketplace.marketplaceservice.repository;

import com.onlinemarketplace.marketplaceservice.model.Order;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    @Query("SELECT o FROM Order o WHERE o.user_id = :user_id")
    List<Order> findAllByUserId(Integer user_id);

    @Transactional
    @Modifying
    @Query("UPDATE Order o SET o.status = :status WHERE o.order_id = :order_id")
    void updateStatusByOrder_id(Integer order_id, String status);
}
