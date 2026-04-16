package org.wiki.service;

import cn.hutool.core.util.EnumUtil;
import org.wiki.constant.OrderStatusEnum;

import java.util.List;
import java.util.Map;

public class EnumService {

    public static void main(String[] args) {
        //1. 一键获取所有枚举名称：getNames ()
        List<String> names = EnumUtil.getNames(OrderStatusEnum.class);
        //2. 批量提取枚举字段值：getFieldValues ()
        List<Object> code = EnumUtil.getFieldValues(OrderStatusEnum.class, "code");
        //3. 按条件找枚举对象：getBy ()
        OrderStatusEnum paidStatus = EnumUtil.getBy(OrderStatusEnum::getCode, 200);
        //6. 枚举名称→字段值映射：getNameFieldMap ()
        Map<String, Object> fieldMap = EnumUtil.getNameFieldMap(OrderStatusEnum.class, "code");
    }
}
