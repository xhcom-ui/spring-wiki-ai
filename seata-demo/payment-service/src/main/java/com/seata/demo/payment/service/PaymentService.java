package com.seata.demo.payment.service;

import com.seata.demo.common.response.Response;

import java.math.BigDecimal;

public interface PaymentService {
    Response<Void> create(Long userId, BigDecimal amount);
}
