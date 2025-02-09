package com.onlinemarketplace.accountservice.repository;

import com.onlinemarketplace.accountservice.model.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserID(Long userId);

    Optional<User> findByEmail(String userEmail);

    @Transactional
    @Modifying
    void deleteByUserID(Long userId);

    @Transactional
    @Modifying
    void deleteAll();
}
