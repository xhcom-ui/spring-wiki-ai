package com.example.flowlong.repository;

import com.example.flowlong.controller.dto.FormCatalogQueryRequest;
import com.example.flowlong.entity.FormCatalog;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

public interface FormCatalogRepository {
    FormCatalog selectById(Long id);

    FormCatalog findByFormKey(String formKey);

    List<FormCatalog> findAll();

    List<FormCatalog> findByStatus(Integer status);

    List<FormCatalog> findPageByCondition(@Param("query") FormCatalogQueryRequest query);

    long countByCondition(@Param("query") FormCatalogQueryRequest query);

    int insert(FormCatalog formCatalog);

    int update(FormCatalog formCatalog);

    int removeById(Long id);

    default Optional<FormCatalog> findById(Long id) {
        return Optional.ofNullable(selectById(id));
    }

    default FormCatalog save(FormCatalog formCatalog) {
        if (formCatalog.getId() == null) {
            insert(formCatalog);
        } else {
            update(formCatalog);
        }
        return formCatalog;
    }

    default List<FormCatalog> saveAll(List<FormCatalog> formCatalogs) {
        for (FormCatalog formCatalog : formCatalogs) {
            save(formCatalog);
        }
        return formCatalogs;
    }

    default void delete(FormCatalog formCatalog) {
        if (formCatalog != null && formCatalog.getId() != null) {
            removeById(formCatalog.getId());
        }
    }
}
