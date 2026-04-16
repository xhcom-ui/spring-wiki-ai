package com.seata.demo.common.request;

import lombok.Data;

@Data
public class OrderRequest {
    private Long userId;
    private Long productId;
    private Integer count;
}
