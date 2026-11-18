package com.rit.token.ritz.application;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegisteredApplicationRepository extends JpaRepository<RegisteredApplication, Long> {
    Optional<RegisteredApplication> findByApplicationId(String applicationId);
    Optional<RegisteredApplication> findByApiKey(String apiKey);
    boolean existsByApplicationId(String applicationId);
}
