package com.seata.demo.order.service.impl;

import com.seata.demo.common.feign.InventoryFeignClient;
import com.seata.demo.common.feign.PaymentFeignClient;
import com.seata.demo.common.request.OrderRequest;
import com.seata.demo.common.response.Response;
import com.seata.demo.order.entity.Order;
import com.seata.demo.order.mapper.OrderMapper;
import com.seata.demo.order.service.OrderService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private InventoryFeignClient inventoryFeignClient;

    @Resource
    private PaymentFeignClient paymentFeignClient;

    @Override
    @GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
    public Response<Long> createOrder(OrderRequest request) {
        log.info("开始创建订单");

        // 1. 创建订单
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setProductId(request.getProductId());
        order.setCount(request.getCount());
        // 假设商品价格为100元
        BigDecimal amount = new BigDecimal(100).multiply(new BigDecimal(request.getCount()));
        order.setAmount(amount);
        order.setStatus("CREATED");
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);
        log.info("订单创建成功，订单ID: {}", order.getId());

        // 2. 扣减库存
        log.info("开始扣减库存");
        Response<Void> inventoryResponse = inventoryFeignClient.deduct(request.getProductId(), request.getCount());
        if (inventoryResponse.getCode() != 200) {
            throw new RuntimeException("扣减库存失败: " + inventoryResponse.getMessage());
        }
        log.info("库存扣减成功");

        // 3. 创建支付
        log.info("开始创建支付");
        Response<Void> paymentResponse = paymentFeignClient.create(request.getUserId(), amount);
        if (paymentResponse.getCode() != 200) {
            throw new RuntimeException("创建支付失败: " + paymentResponse.getMessage());
        }
        log.info("支付创建成功");

        // 4. 更新订单状态
        order.setStatus("PAID");
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("订单状态更新成功");

        return Response.success(order.getId());
    }
}
