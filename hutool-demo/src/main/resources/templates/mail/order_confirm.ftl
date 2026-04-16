<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { background: #4CAF50; color: white; padding: 20px; text-align: center; }
        .content { padding: 20px; background: #f9f9f9; }
        .order-info { margin: 20px 0; }
        .order-info table { width: 100%; border-collapse: collapse; }
        .order-info th, .order-info td { padding: 10px; text-align: left; border-bottom: 1px solid #ddd; }
        .order-info th { background: #f0f0f0; font-weight: bold; }
        .total { font-size: 18px; font-weight: bold; color: #4CAF50; text-align: right; margin-top: 10px; }
        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>订单确认</h1>
        </div>

        <div class="content">
            <p>尊敬的 ${customerName}，您好！</p>
            <p>感谢您的订购，您的订单已确认，详情如下：</p>

            <div class="order-info">
                <table>
                    <tr>
                        <th>订单号</th>
                        <td>${orderNo}</td>
                    </tr>
                    <tr>
                        <th>下单时间</th>
                        <td>${orderTime?string("yyyy-MM-dd HH:mm:ss")}</td>
                    </tr>
                    <tr>
                        <th>配送地址</th>
                        <td>${deliveryAddress}</td>
                    </tr>
                    <tr>
                        <th>联系电话</th>
                        <td>${phone}</td>
                    </tr>
                </table>

                <h3>商品清单</h3>
                <table>
                    <tr>
                        <th>商品名称</th>
                        <th>规格</th>
                        <th>单价</th>
                        <th>数量</th>
                        <th>小计</th>
                    </tr>
                    <#list items as item>
                    <tr>
                        <td>${item.productName}</td>
                        <td>${item.spec}</td>
                        <td>¥${item.price}</td>
                        <td>${item.quantity}</td>
                        <td>¥${item.subtotal}</td>
                    </tr>
                    </#list>
                </table>

                <div class="total">
                    订单总额：¥${totalAmount}
                </div>
            </div>

            <p>预计送达时间：${estimatedDeliveryTime}</p>
            <p>如有任何问题，请联系客服：400-123-4567</p>
        </div>

        <div class="footer">
            <p>此邮件由系统自动发送，请勿直接回复</p>
            <p>&copy; 2024 咖啡订单系统 版权所有</p>
        </div>
    </div>
</body>
</html>