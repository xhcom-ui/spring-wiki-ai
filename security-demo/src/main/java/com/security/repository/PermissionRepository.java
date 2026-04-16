package com.security.repository;

import com.security.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 权限Repository
 */
public interface PermissionRepository extends JpaRepository<Permission, Long> {

}
