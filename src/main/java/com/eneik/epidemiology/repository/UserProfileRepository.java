package com.eneik.epidemiology.repository;

import com.eneik.epidemiology.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
}
