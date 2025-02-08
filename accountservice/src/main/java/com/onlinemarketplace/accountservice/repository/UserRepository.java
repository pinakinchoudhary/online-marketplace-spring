package com.onlinemarketplace.accountservice.repository;

import com.onlinemarketplace.accountservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT b FROM User b WHERE b.id = :id")
    User findByUserId(Long id);
}
