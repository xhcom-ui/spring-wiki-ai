package com.seata.demo.inventory.service.impl;

import com.seata.demo.common.response.Response;
import com.seata.demo.inventory.mapper.InventoryMapper;
import com.seata.demo.inventory.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    @Resource
    private InventoryMapper inventoryMapper;

    @Override
    public Response<Void> deduct(Long productId, Integer count) {
        log.info("开始扣减库存，商品ID: {}, 数量: {}", productId, count);

        int result = inventoryMapper.deductStock(productId, count);
        if (result == 0) {
            log.error("库存不足，商品ID: {}, 数量: {}", productId, count);
            return Response.error("库存不足");
        }

        log.info("库存扣减成功，商品ID: {}, 数量: {}", productId, count);
        return Response.success(null);
    }
}
