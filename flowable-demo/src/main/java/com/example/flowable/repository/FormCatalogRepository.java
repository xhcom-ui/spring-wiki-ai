package com.example.flowable.repository;

import com.example.flowable.entity.FormCatalog;

import java.util.List;
import java.util.Optional;

public interface FormCatalogRepository {
    FormCatalog selectById(Long id);

    FormCatalog findByFormKey(String formKey);

    List<FormCatalog> findByStatus(Integer status);

    List<FormCatalog> findAll();

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

    default void delete(FormCatalog formCatalog) {
        if (formCatalog != null && formCatalog.getId() != null) {
            removeById(formCatalog.getId());
        }
    }
}
