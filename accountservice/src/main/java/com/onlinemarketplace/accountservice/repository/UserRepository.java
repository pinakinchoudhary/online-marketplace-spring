package com.onlinemarketplace.accountservice.repository;

import com.onlinemarketplace.accountservice.model.AccountUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountUserRepository extends JpaRepository<AccountUser, Long> {

    @Query("SELECT b FROM AccountUser b WHERE b.id = :id")
    Optional<AccountUser> findById(Long id);
}
