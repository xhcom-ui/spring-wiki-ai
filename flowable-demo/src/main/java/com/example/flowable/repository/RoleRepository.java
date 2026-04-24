package com.example.flowable.repository;

import com.example.flowable.entity.Role;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

public interface RoleRepository {
    Role selectById(Long id);

    Role findByCode(String code);

    List<Role> findAll();

    List<Role> findAllById(@Param("ids") List<Long> ids);

    int insert(Role role);

    int update(Role role);

    int removeById(Long id);

    default Optional<Role> findById(Long id) {
        return Optional.ofNullable(selectById(id));
    }

    default Role save(Role role) {
        if (role.getId() == null) {
            insert(role);
        } else {
            update(role);
        }
        return role;
    }

    default List<Role> saveAll(List<Role> roles) {
        for (Role role : roles) {
            save(role);
        }
        return roles;
    }

    default void delete(Role role) {
        if (role != null && role.getId() != null) {
            removeById(role.getId());
        }
    }
}
