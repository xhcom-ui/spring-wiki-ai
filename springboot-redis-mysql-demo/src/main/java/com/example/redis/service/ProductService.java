package com.example.redis.service;

import com.example.redis.entity.Product;
import java.util.List;

public interface ProductService {
    Product save(Product product);
    Product findById(Long id);
    List<Product> findAll();
    void deleteById(Long id);
    Product update(Product product);
}
