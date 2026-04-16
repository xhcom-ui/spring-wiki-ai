package com.seata.demo.payment.service.impl;

import com.seata.demo.common.response.Response;
import com.seata.demo.payment.entity.Payment;
import com.seata.demo.payment.mapper.PaymentMapper;
import com.seata.demo.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    @Resource
    private PaymentMapper paymentMapper;

    @Override
    public Response<Void> create(Long userId, BigDecimal amount) {
        log.info("开始创建支付，用户ID: {}, 金额: {}", userId, amount);

        Payment payment = new Payment();
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setStatus("PENDING");
        payment.setCreateTime(LocalDateTime.now());
        payment.setUpdateTime(LocalDateTime.now());
        paymentMapper.insert(payment);

        // 模拟支付处理
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.error("支付处理异常", e);
            return Response.error("支付处理异常");
        }

        // 更新支付状态
        payment.setStatus("SUCCESS");
        payment.setUpdateTime(LocalDateTime.now());
        paymentMapper.updateById(payment);

        log.info("支付创建成功，支付ID: {}", payment.getId());
        return Response.success(null);
    }
}
