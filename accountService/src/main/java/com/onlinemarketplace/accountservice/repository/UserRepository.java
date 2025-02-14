package com.onlinemarketplace.accountservice.repository;

import com.onlinemarketplace.accountservice.model.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String userEmail);

    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.discount_availed = :discountValid WHERE u.id = :id")
    void updateDiscountValidById(int id, boolean discountAvailed);

}
