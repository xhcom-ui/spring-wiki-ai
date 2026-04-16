package com.seata.demo.common.feign;

import com.seata.demo.common.response.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory-service")
public interface InventoryFeignClient {
    @PostMapping("/inventory/deduct")
    Response<Void> deduct(@RequestParam("productId") Long productId, @RequestParam("count") Integer count);
}
