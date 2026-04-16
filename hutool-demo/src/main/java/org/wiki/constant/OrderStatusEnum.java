package org.wiki.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单主状态枚举
 */
@Getter
@AllArgsConstructor
public enum OrderStatusEnum {

    /**
     * 待支付
     */
    PENDING_PAY(0, "待支付"),

    /**
     * 待确认/待接单
     */
    PENDING_CONFIRM(1, "待确认"),

    /**
     * 配送中
     */
    DELIVERING(2, "配送中"),

    /**
     * 已完成
     */
    COMPLETED(3, "已完成"),

    /**
     * 已取消
     */
    CANCELED(4, "已取消"),

    /**
     * 已关闭
     */
    CLOSED(5, "已关闭");

    private final Integer code;
    private final String desc;
}