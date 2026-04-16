package com.seata.demo.inventory.controller;

import com.seata.demo.common.response.Response;
import com.seata.demo.inventory.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/inventory")
@Slf4j
public class InventoryController {

    @Resource
    private InventoryService inventoryService;

    @PostMapping("/deduct")
    public Response<Void> deduct(@RequestParam("productId") Long productId, @RequestParam("count") Integer count) {
        log.info("接收到扣减库存请求: 商品ID={}, 数量={}", productId, count);
        return inventoryService.deduct(productId, count);
    }
}
