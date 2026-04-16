package org.wiki.entity;


import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单传输对象（根据 EmailService 反向推理）
 */
@Data
public class OrderDTO {

    // ====================== 基础订单信息 ======================
    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 客户姓名
     */
    private String customerName;

    /**
     * 客户邮箱（接收邮件）
     */
    private String customerEmail;

    /**
     * 订单创建时间
     */
    private LocalDateTime orderTime;

    /**
     * 配送地址
     */
    private String deliveryAddress;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    // ====================== 订单商品明细（嵌套集合） ======================
    /**
     * 订单商品列表
     * 内部属性：productName、spec、price、quantity
     */
    private List<OrderItemDTO> items;

    // ============================ 内部嵌套类：订单项 ============================
    @Data
    public static class OrderItemDTO {
        /**
         * 商品名称
         */
        private String productName;

        /**
         * 商品规格
         */
        private String spec;

        /**
         * 商品单价
         */
        private BigDecimal price;

        /**
         * 购买数量
         */
        private Integer quantity;
    }
}