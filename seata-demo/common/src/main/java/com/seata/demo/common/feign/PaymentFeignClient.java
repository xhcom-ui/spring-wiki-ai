package com.seata.demo.common.feign;

import com.seata.demo.common.response.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "payment-service")
public interface PaymentFeignClient {
    @PostMapping("/payment/create")
    Response<Void> create(@RequestParam("userId") Long userId, @RequestParam("amount") BigDecimal amount);
}
