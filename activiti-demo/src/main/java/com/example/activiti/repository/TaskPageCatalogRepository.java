package com.example.activiti.repository;

import com.example.activiti.entity.TaskPageCatalogItem;

import java.util.List;
import java.util.Optional;

public interface TaskPageCatalogRepository {
    TaskPageCatalogItem selectById(Long id);

    TaskPageCatalogItem findByItemKey(String itemKey);

    List<TaskPageCatalogItem> findAll();

    List<TaskPageCatalogItem> findByStatus(Integer status);

    int insert(TaskPageCatalogItem item);

    int update(TaskPageCatalogItem item);

    int removeById(Long id);

    default Optional<TaskPageCatalogItem> findById(Long id) {
        return Optional.ofNullable(selectById(id));
    }

    default TaskPageCatalogItem save(TaskPageCatalogItem item) {
        if (item.getId() == null) {
            insert(item);
        } else {
            update(item);
        }
        return item;
    }

    default List<TaskPageCatalogItem> saveAll(List<TaskPageCatalogItem> items) {
        for (TaskPageCatalogItem item : items) {
            save(item);
        }
        return items;
    }
}
