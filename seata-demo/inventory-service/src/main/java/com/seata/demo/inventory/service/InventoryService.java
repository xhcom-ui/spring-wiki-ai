package com.seata.demo.inventory.service;

import com.seata.demo.common.response.Response;

public interface InventoryService {
    Response<Void> deduct(Long productId, Integer count);
}
