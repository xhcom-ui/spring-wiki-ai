package org.wiki.service;

import cn.hutool.core.swing.RobotUtil;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;

public class UITestHelper {
    private final String screenshotDir;

    public UITestHelper(String screenshotDir) {
        this.screenshotDir = screenshotDir;
        new File(screenshotDir).mkdirs();
    }

    /**
     * 点击并截图
     */
    public void clickAndCapture(int x, int y, String stepName) {
        // 移动并点击
        RobotUtil.mouseMove(x, y);
        RobotUtil.delay();
        RobotUtil.click();

        // 等待响应
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 截图记录
        String fileName = stepName + "_" + System.currentTimeMillis() + ".png";
        RobotUtil.captureScreen(new File(screenshotDir, fileName));
        System.out.println("步骤完成: " + stepName);
    }

    /**
     * 输入文本并截图
     */
    public void typeAndCapture(String text, String stepName) {
        RobotUtil.keyPressString(text);

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String fileName = stepName + "_" + System.currentTimeMillis() + ".png";
        RobotUtil.captureScreen(new File(screenshotDir, fileName));
        System.out.println("输入完成: " + stepName);
    }

    /**
     * 执行测试流程
     */
//    public void runTest(List<TestStep> steps) {
//        for (TestStep step : steps) {
//            switch (step.getAction()) {
//                case "click":
//                    clickAndCapture(step.getX(), step.getY(), step.getName());
//                    break;
//                case "type":
//                    typeAndCapture(step.getText(), step.getName());
//                    break;
//                case "key":
//                    RobotUtil.keyClick(step.getKeyCode());
//                    break;
//            }
//            RobotUtil.delay();
//        }
//    }
    public static void main(String[] args) {
        // 1. 全屏截图
        BufferedImage img = RobotUtil.captureScreen();

        // 2. 截图保存到文件
        RobotUtil.captureScreen(new File("screenshot.png"));

        // 3. 区域截图
        Rectangle rect = new Rectangle(0, 0, 800, 600);
        RobotUtil.captureScreen(rect, new File("area.png"));

        // 4. 鼠标操作
        RobotUtil.mouseMove(500, 300);
        RobotUtil.click();
        RobotUtil.rightClick();
        RobotUtil.mouseWheel(3);

        // 5. 键盘操作
        RobotUtil.keyClick(KeyEvent.VK_ENTER);
        RobotUtil.keyPressWithCtrl(KeyEvent.VK_C);
        RobotUtil.keyPressString("Hello World");

        // 6. 设置延迟
        RobotUtil.setDelay(500);
        RobotUtil.delay();
    }
}