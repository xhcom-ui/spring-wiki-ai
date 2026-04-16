package org.wiki.service;

import cn.hutool.extra.template.Template;
import cn.hutool.extra.template.TemplateEngine;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.wiki.entity.OrderDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * 发送订单确认邮件
     */
    public void sendOrderConfirmEmail(OrderDTO order) {
        try {
            // 准备模板数据
            Map<String, Object> data = buildTemplateData(order);

            // 渲染模板
            Template template = templateEngine.getTemplate("order_confirm.ftl");
            String htmlContent = template.render(data);

            // 发送邮件
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(order.getCustomerEmail());
            helper.setSubject("订单确认 - " + order.getOrderNo());
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("订单确认邮件发送成功，订单号：{}", order.getOrderNo());

        } catch (Exception e) {
            log.error("订单确认邮件发送失败，订单号：{}", order.getOrderNo(), e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    /**
     * 构建模板数据
     */
    private Map<String, Object> buildTemplateData(OrderDTO order) {
        Map<String, Object> data = new HashMap<>();

        data.put("customerName", order.getCustomerName());
        data.put("orderNo", order.getOrderNo());
        data.put("orderTime", order.getOrderTime());
        data.put("deliveryAddress", order.getDeliveryAddress());
        data.put("phone", order.getPhone());

        // 商品清单
        List<Map<String, Object>> items = order.getItems().stream()
            .map(item -> {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("productName", item.getProductName());
                itemMap.put("spec", item.getSpec());
                itemMap.put("price", item.getPrice());
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("subtotal", item.getPrice().multiply(
                    BigDecimal.valueOf(item.getQuantity())));
                return itemMap;
            })
            .toList();
        data.put("items", items);

        data.put("totalAmount", order.getTotalAmount());

        LocalDateTime estimatedTime = order.getOrderTime().plusMinutes(30);
        data.put("estimatedDeliveryTime",
            estimatedTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        return data;
    }
}