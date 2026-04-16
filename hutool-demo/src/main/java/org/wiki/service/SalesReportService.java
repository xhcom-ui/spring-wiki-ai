package org.wiki.service;

import cn.hutool.poi.word.TableUtil;
import lombok.Data;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class SalesReportService {

    /**
     * 生成月度销售报表
     */
    public void generateMonthlySalesReport(int year, int month) throws Exception {
        // 创建 Word 文档
        XWPFDocument doc = new XWPFDocument();

        // 添加标题
        XWPFParagraph title = doc.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = title.createRun();
        titleRun.setText(year + "年" + month + "月销售报表");
        titleRun.setFontSize(18);
        titleRun.setBold(true);

        // 添加生成时间
        XWPFParagraph datePara = doc.createParagraph();
        XWPFRun dateRun = datePara.createRun();
        dateRun.setText("生成时间：" + LocalDate.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        dateRun.setFontSize(10);

        // 空行
        doc.createParagraph();

        // 第一部分：商品销售统计
        XWPFParagraph section1 = doc.createParagraph();
        XWPFRun section1Run = section1.createRun();
        section1Run.setText("一、商品销售统计");
        section1Run.setFontSize(14);
        section1Run.setBold(true);

        // 从数据库查询商品销售数据（模拟）
        List<ProductSales> productSalesList = queryProductSales(year, month);

        // 构建表格数据
        List<List<Object>> productData = new ArrayList<>();
        productData.add(List.of("排名", "商品名称", "销售数量", "销售金额", "占比"));

        int rank = 1;
        for (ProductSales sales : productSalesList) {
            productData.add(List.of(
                rank++,
                sales.getProductName(),
                sales.getQuantity(),
                sales.getAmount().toString(),
                sales.getPercentage() + "%"
            ));
        }

        // 创建商品销售表格
        XWPFTable productTable = TableUtil.createTable(doc);
        TableUtil.writeTable(productTable, productData);

        // 设置表格样式
        productTable.setWidth("100%");

        // 空行
        doc.createParagraph();

        // 第二部分：员工业绩排名
        XWPFParagraph section2 = doc.createParagraph();
        XWPFRun section2Run = section2.createRun();
        section2Run.setText("二、员工业绩排名");
        section2Run.setFontSize(14);
        section2Run.setBold(true);

        // 从数据库查询员工业绩数据（模拟）
        List<EmployeePerformance> performanceList = queryEmployeePerformance(year, month);

        // 构建员工业绩表格
        XWPFTable empTable = TableUtil.createTable(doc);

        // 写入表头
        XWPFTableRow headerRow = empTable.getRow(0);
        List<String> headers = List.of("排名", "员工姓名", "接单数", "销售额", "客户评分");
        TableUtil.writeRow(headerRow, headers);

        // 逐行写入员工数据
        int empRank = 1;
        for (EmployeePerformance perf : performanceList) {
            XWPFTableRow dataRow = TableUtil.getOrCreateRow(empTable, empRank);
            List<Object> rowData = List.of(
                empRank,
                perf.getEmployeeName(),
                perf.getOrderCount(),
                perf.getSalesAmount().toString(),
                perf.getRating()
            );
            TableUtil.writeRow(dataRow, rowData);
            empRank++;
        }

        // 设置表格样式
        empTable.setWidth("100%");

        // 空行
        doc.createParagraph();

        // 第三部分：总结
        XWPFParagraph summary = doc.createParagraph();
        XWPFRun summaryRun = summary.createRun();
        summaryRun.setText("三、数据总结");
        summaryRun.setFontSize(14);
        summaryRun.setBold(true);

        // 计算汇总数据
        BigDecimal totalAmount = productSalesList.stream()
            .map(ProductSales::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalOrders = performanceList.stream()
            .mapToInt(EmployeePerformance::getOrderCount)
            .sum();

        // 创建汇总表格
        XWPFTable summaryTable = TableUtil.createTable(doc);
        List<List<String>> summaryData = List.of(
            List.of("指标", "数值"),
            List.of("总销售额", totalAmount.toString() + " 元"),
            List.of("总订单数", totalOrders + " 单"),
            List.of("客单价", totalAmount.divide(
                BigDecimal.valueOf(totalOrders), 2, BigDecimal.ROUND_HALF_UP
            ).toString() + " 元")
        );
        TableUtil.writeTable(summaryTable, summaryData);

        // 保存文档
        String fileName = String.format("销售报表_%d年%d月.docx", year, month);
        try (FileOutputStream out = new FileOutputStream(fileName)) {
            doc.write(out);
        }

        System.out.println("报表生成成功：" + fileName);
    }

    // 模拟数据库查询方法
    private List<ProductSales> queryProductSales(int year, int month) {
        List<ProductSales> list = new ArrayList<>();
        list.add(new ProductSales("美式咖啡", 320, new BigDecimal("9600.00"), "35.2"));
        list.add(new ProductSales("拿铁咖啡", 280, new BigDecimal("11200.00"), "41.0"));
        list.add(new ProductSales("卡布奇诺", 150, new BigDecimal("6000.00"), "22.0"));
        list.add(new ProductSales("摩卡咖啡", 50, new BigDecimal("2500.00"), "9.2"));
        return list;
    }

    private List<EmployeePerformance> queryEmployeePerformance(int year, int month) {
        List<EmployeePerformance> list = new ArrayList<>();
        list.add(new EmployeePerformance("张三", 180, new BigDecimal("18000.00"), 4.8));
        list.add(new EmployeePerformance("李四", 150, new BigDecimal("15500.00"), 4.6));
        list.add(new EmployeePerformance("王五", 120, new BigDecimal("12000.00"), 4.5));
        return list;
    }
}

// 商品销售数据实体
@Data
class ProductSales {
    private String productName;
    private Integer quantity;
    private BigDecimal amount;
    private String percentage;

    public ProductSales(String productName, Integer quantity,
                       BigDecimal amount, String percentage) {
        this.productName = productName;
        this.quantity = quantity;
        this.amount = amount;
        this.percentage = percentage;
    }
}

// 员工业绩数据实体
@Data
class EmployeePerformance {
    private String employeeName;
    private Integer orderCount;
    private BigDecimal salesAmount;
    private Double rating;

    public EmployeePerformance(String employeeName, Integer orderCount,
                              BigDecimal salesAmount, Double rating) {
        this.employeeName = employeeName;
        this.orderCount = orderCount;
        this.salesAmount = salesAmount;
        this.rating = rating;
    }
}