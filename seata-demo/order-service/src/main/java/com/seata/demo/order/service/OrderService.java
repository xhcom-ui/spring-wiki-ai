package com.seata.demo.order.service;

import com.seata.demo.common.request.OrderRequest;
import com.seata.demo.common.response.Response;

public interface OrderService {
    Response<Long> createOrder(OrderRequest request);
}
