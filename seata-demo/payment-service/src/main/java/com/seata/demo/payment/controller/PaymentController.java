package com.seata.demo.payment.controller;

import com.seata.demo.common.response.Response;
import com.seata.demo.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.math.BigDecimal;

@RestController
@RequestMapping("/payment")
@Slf4j
public class PaymentController {

    @Resource
    private PaymentService paymentService;

    @PostMapping("/create")
    public Response<Void> create(@RequestParam("userId") Long userId, @RequestParam("amount") BigDecimal amount) {
        log.info("接收到创建支付请求: 用户ID={}, 金额={}", userId, amount);
        return paymentService.create(userId, amount);
    }
}
