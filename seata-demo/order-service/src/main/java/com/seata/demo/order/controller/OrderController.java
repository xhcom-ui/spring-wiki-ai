package com.seata.demo.order.controller;

import com.seata.demo.common.request.OrderRequest;
import com.seata.demo.common.response.Response;
import com.seata.demo.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/order")
@Slf4j
public class OrderController {

    @Resource
    private OrderService orderService;

    @PostMapping("/create")
    public Response<Long> createOrder(@RequestBody OrderRequest request) {
        log.info("接收到创建订单请求: {}", request);
        return orderService.createOrder(request);
    }
}
