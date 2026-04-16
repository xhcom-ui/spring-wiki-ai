package com.security.repository;

import com.security.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 角色Repository
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

}
