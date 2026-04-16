package org.wiki.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.poi.word.TableUtil;

import cn.hutool.poi.word.TableUtil;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class TableService {

    public static void main(String[] args) throws FileNotFoundException {
        // 创建 Word 文档对象
        XWPFDocument doc = new XWPFDocument();

        // 创建空表格（默认只有一行）
        XWPFTable table = TableUtil.createTable(doc);

        // 保存文档
        try (FileOutputStream out = new FileOutputStream("empty_table.docx")) {
            doc.write(out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        // 准备数据（List 集合）
        List<List<String>> data = CollUtil.newArrayList(
                CollUtil.newArrayList("姓名", "部门", "职位"),
                CollUtil.newArrayList("张三", "技术部", "Java 工程师"),
                CollUtil.newArrayList("李四", "产品部", "产品经理"),
                CollUtil.newArrayList("王五", "运营部", "运营专员")
        );

        XWPFDocument doc1 = new XWPFDocument();

       // 创建表格并填充数据
        XWPFTable table1 = TableUtil.createTable(doc1, data);

        try (FileOutputStream out = new FileOutputStream("employee_table.docx")) {
            doc1.write(out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
