package com.eneik.epidemiology.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    @Query("SELECT u.role FROM User u WHERE u.username = :username")
    Optional<String> findRoleByUsername(@Param("username") String username);

    @Query("SELECT u.role FROM User u WHERE u.id = :id")
    Optional<String> findRoleById(@Param("id") Long id);
}
