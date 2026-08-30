package com.eneik.epidemiology.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByMoodleId(String moodleId);

    @Modifying
    @Query("UPDATE User u SET u.role = :newRole WHERE u.id = :id AND u.role = :oldRole")
    int updateRoleAtomically(@Param("id") Long id, @Param("oldRole") String oldRole, @Param("newRole") String newRole);

    @Modifying
    @Query("UPDATE User u SET u.role = :newRole, u.department = :newDepartment, u.courses = :courses WHERE u.id = :id AND (u.role = :oldRole OR u.department IS NULL OR u.department != :newDepartment OR (:courses IS NULL OR u.courses IS NULL OR u.courses != :courses))")
    int updateRoleAndDepartmentAtomically(@Param("id") Long id, @Param("oldRole") String oldRole, @Param("newRole") String newRole, @Param("newDepartment") String newDepartment, @Param("courses") String courses);

    @Query("SELECT u.role FROM User u WHERE u.username = :username")
    Optional<String> findRoleByUsername(@Param("username") String username);

    @Query("SELECT u.role FROM User u WHERE u.id = :id")
    Optional<String> findRoleById(@Param("id") Long id);
}
