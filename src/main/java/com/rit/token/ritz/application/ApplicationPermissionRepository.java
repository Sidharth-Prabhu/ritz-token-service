package com.rit.token.ritz.application;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationPermissionRepository extends JpaRepository<ApplicationPermission, Long> {
    List<ApplicationPermission> findByApplicationId(String applicationId);
    boolean existsByApplicationIdAndPermissionScope(String applicationId, PermissionScope permissionScope);
    void deleteByApplicationId(String applicationId);
}
