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

    /**
     * Finds a user by their email.
     *
     * @param userEmail the email of the user to search for
     * @return an {@link Optional} containing the found user, or empty if no user is found
     */
    Optional<User> findByEmail(String userEmail);
    // Spring Data JPA's findByEmail() returns an Optional<User>, allowing safer null handling
    // and preventing NullPointerExceptions by forcing explicit checks before accessing the value.


    /**
     * Updates the discount availed status of a user by their ID.
     * Uses a custom JPQL query to perform the update.
     *
     * @param id the ID of the user whose discount status needs to be updated
     * @param discountAvailed the new discount availed status to be set
     */
    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.discount_availed = :discountAvailed WHERE u.id = :id")
    void updateDiscountAvailedByIdById(int id, boolean discountAvailed);
}
