package org.wiki.service;

import cn.hutool.poi.excel.style.StyleUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public class OrderReportExportDemo {

    static class OrderRow {
        String orderNo;
        String userName;
        BigDecimal amount;
        String status;

        OrderRow(String orderNo, String userName, BigDecimal amount, String status) {
            this.orderNo = orderNo;
            this.userName = userName;
            this.amount = amount;
            this.status = status;
        }
    }

    public static void main(String[] args) throws Exception {
        List<OrderRow> rows = Arrays.asList(
            new OrderRow("OD20260315001", "张三", new BigDecimal("199.90"), "已支付"),
            new OrderRow("OD20260315002", "李四", new BigDecimal("89.00"), "退款中"),
            new OrderRow("OD20260315003", "王五", new BigDecimal("560.50"), "已支付")
        );

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("订单日报");

        // 1. 基础样式
        CellStyle headStyle = StyleUtil.createHeadCellStyle(workbook);
        CellStyle bodyStyle = StyleUtil.createDefaultCellStyle(workbook);

        // 2. 表头样式细化：居中 + 边框 + 深色填充 + 白字
        StyleUtil.setAlign(headStyle, HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
        StyleUtil.setBorder(headStyle, BorderStyle.THIN, IndexedColors.GREY_50_PERCENT);
        StyleUtil.setColor(headStyle, IndexedColors.DARK_BLUE, FillPatternType.SOLID_FOREGROUND);
        Font headFont = StyleUtil.createFont(workbook, IndexedColors.WHITE.getIndex(), (short) 11, "微软雅黑");
        headStyle.setFont(headFont);

        // 3. 内容样式细化：左对齐 + 边框
        StyleUtil.setAlign(bodyStyle, HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
        StyleUtil.setBorder(bodyStyle, BorderStyle.THIN, IndexedColors.GREY_25_PERCENT);

        // 4. 金额样式：克隆内容样式 + 数字格式
        CellStyle amountStyle = StyleUtil.cloneCellStyle(workbook, bodyStyle);
        Short moneyFormat = StyleUtil.getFormat(workbook, "#,##0.00");
        amountStyle.setDataFormat(moneyFormat);
        StyleUtil.setAlign(amountStyle, HorizontalAlignment.RIGHT, VerticalAlignment.CENTER);

        // 5. 异常行样式：基于内容样式克隆，高亮背景
        CellStyle warningStyle = StyleUtil.cloneCellStyle(workbook, bodyStyle);
        StyleUtil.setColor(warningStyle, IndexedColors.ROSE.getIndex(), FillPatternType.SOLID_FOREGROUND);
        Font warningFont = StyleUtil.createFont(workbook, IndexedColors.RED.getIndex(), (short) 10, "微软雅黑");
        StyleUtil.setFontStyle(warningFont, IndexedColors.RED.getIndex(), (short) 10, "微软雅黑");
        warningStyle.setFont(warningFont);

        // 标题行
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("电商订单日报");
        CellStyle titleStyle = StyleUtil.cloneCellStyle(workbook, headStyle);
        Font titleFont = StyleUtil.createFont(workbook, IndexedColors.WHITE.getIndex(), (short) 14, "微软雅黑");
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 3));

        // 表头行
        Row headRow = sheet.createRow(1);
        String[] heads = {"订单号", "用户", "金额", "状态"};
        for (int i = 0; i < heads.length; i++) {
            Cell cell = headRow.createCell(i);
            cell.setCellValue(heads[i]);
            cell.setCellStyle(headStyle);
        }

        // 数据行
        int rowIndex = 2;
        for (OrderRow item : rows) {
            Row row = sheet.createRow(rowIndex++);

            Cell c0 = row.createCell(0);
            c0.setCellValue(item.orderNo);

            Cell c1 = row.createCell(1);
            c1.setCellValue(item.userName);

            Cell c2 = row.createCell(2);
            c2.setCellValue(item.amount.doubleValue());

            Cell c3 = row.createCell(3);
            c3.setCellValue(item.status);

            boolean warning = "退款中".equals(item.status);
            CellStyle rowStyle = warning ? warningStyle : bodyStyle;

            c0.setCellStyle(rowStyle);
            c1.setCellStyle(rowStyle);
            c3.setCellStyle(rowStyle);

            // 金额列单独使用金额样式；若是异常行，则在金额样式基础上再克隆一份高亮
            if (warning) {
                CellStyle warningAmountStyle = StyleUtil.cloneCellStyle(c2, amountStyle);
                StyleUtil.setColor(warningAmountStyle, IndexedColors.ROSE, FillPatternType.SOLID_FOREGROUND);
                c2.setCellStyle(warningAmountStyle);
            } else {
                c2.setCellStyle(amountStyle);
            }
        }

        // 判断某个样式是否为默认样式（示例）
        boolean isDefault = StyleUtil.isNullOrDefaultStyle(workbook, workbook.getCellStyleAt(0));
        System.out.println("workbook.getCellStyleAt(0) 是否默认样式：" + isDefault);

        // 列宽
        sheet.setColumnWidth(0, 22 * 256);
        sheet.setColumnWidth(1, 12 * 256);
        sheet.setColumnWidth(2, 12 * 256);
        sheet.setColumnWidth(3, 10 * 256);

        // 导出
        try (FileOutputStream fos = new FileOutputStream("order-report.xlsx")) {
            workbook.write(fos);
        }
        workbook.close();

        System.out.println("导出完成：order-report.xlsx");

        Workbook workbook1 = new XSSFWorkbook();
        CellStyle moneyStyle = StyleUtil.createCellStyle(workbook1);

        Short moneyFormat1 = StyleUtil.getFormat(workbook, "#,##0.00");
        moneyStyle.setDataFormat(moneyFormat1);

        System.out.println("金额格式索引：" + moneyFormat1);


        CellStyle style1 = StyleUtil.createCellStyle(workbook1);
        StyleUtil.setColor(style1, IndexedColors.LIGHT_YELLOW, FillPatternType.SOLID_FOREGROUND);

        CellStyle style2 = StyleUtil.createCellStyle(workbook1);
        StyleUtil.setColor(style2, IndexedColors.ROSE.getIndex(), FillPatternType.SOLID_FOREGROUND);

        System.out.println("颜色样式设置完成");

        CellStyle style = StyleUtil.createCellStyle(workbook1);

        StyleUtil.setBorder(style, BorderStyle.THIN, IndexedColors.GREY_50_PERCENT);

        System.out.println("边框样式：" + style.getBorderTop());


        StyleUtil.setAlign(
                style,
                HorizontalAlignment.CENTER,
                VerticalAlignment.CENTER
        );

        System.out.println("水平对齐：" + style.getAlignment());
        System.out.println("垂直对齐：" + style.getVerticalAlignment());



    }
}