package com.seata.demo.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seata.demo.inventory.entity.Inventory;

public interface InventoryMapper extends BaseMapper<Inventory> {
    int deductStock(Long productId, Integer count);
}
