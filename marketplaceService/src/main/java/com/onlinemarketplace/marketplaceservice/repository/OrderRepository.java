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

    /**
     * Retrieves a list of orders associated with a specific user ID.
     *
     * @param user_id the ID of the user whose orders are to be retrieved
     * @return a list of {@link Order} objects associated with the specified user ID
     */
    @Query("SELECT o FROM Order o WHERE o.user_id = :user_id")
    List<Order> findAllByUserId(Integer user_id);

    /**
     * Updates the status of an order identified by its order ID.
     *
     * @param order_id the ID of the order whose status is to be updated
     * @param status   the new status to set for the order
     */
    @Transactional
    @Modifying
    @Query("UPDATE Order o SET o.status = :status WHERE o.order_id = :order_id")
    void updateStatusByOrder_id(Integer order_id, String status);
}