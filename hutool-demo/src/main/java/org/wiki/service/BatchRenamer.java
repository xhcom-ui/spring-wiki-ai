package org.wiki.service;

import cn.hutool.core.swing.RobotUtil;

import java.awt.event.KeyEvent;
import java.util.List;

//场景3：批量文件重命名
public class BatchRenamer {
/**
     * 在文件管理器中批量重命名（假设已选中文件）
     */
    public void renameFiles(List<String> newNames, int startX, int startY, int itemHeight) {
        RobotUtil.setDelay(200);
        
        for (int i = 0; i < newNames.size(); i++) {
            // 点击文件
            int y = startY + i * itemHeight;
            RobotUtil.mouseMove(startX, y);
            RobotUtil.delay();
            RobotUtil.click();
            RobotUtil.delay();
            
            // F2进入重命名模式
            RobotUtil.keyClick(KeyEvent.VK_F2);
            RobotUtil.delay();
            
            // 全选原文件名
            RobotUtil.keyPressWithCtrl(KeyEvent.VK_A);
            RobotUtil.delay();
            
            // 输入新文件名
            RobotUtil.keyPressString(newNames.get(i));
            RobotUtil.delay();
            
            // 回车确认
            RobotUtil.keyClick(KeyEvent.VK_ENTER);
            RobotUtil.delay();
            
            System.out.println("已重命名为: " + newNames.get(i));
        }
    }
}