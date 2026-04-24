package com.example.flowlong.repository;

import com.example.flowlong.controller.dto.DepartmentQueryRequest;
import com.example.flowlong.entity.Department;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository {
    Department selectById(Long id);

    Department findByCode(String code);

    List<Department> findAll();

    List<Department> findByStatus(Integer status);

    List<Department> findPageByCondition(@Param("query") DepartmentQueryRequest query);

    long countByCondition(@Param("query") DepartmentQueryRequest query);

    long countChildren(Long parentId);

    int insert(Department department);

    int update(Department department);

    int removeById(Long id);

    default Optional<Department> findById(Long id) {
        return Optional.ofNullable(selectById(id));
    }

    default Department save(Department department) {
        if (department.getId() == null) {
            insert(department);
        } else {
            update(department);
        }
        return department;
    }

    default void delete(Department department) {
        if (department != null && department.getId() != null) {
            removeById(department.getId());
        }
    }
}
