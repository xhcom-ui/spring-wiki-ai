package com.security.repository;

import com.security.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 菜单Repository
 */
public interface MenuRepository extends JpaRepository<Menu, Long> {

}
