package org.wiki.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 页面路由 Controller
 */
@Slf4j
@Controller
public class PageController {

    /**
     * 首页 - 对话界面
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * 知识库管理页面
     */
    @GetMapping("/datasets")
    public String datasets() {
        return "datasets";
    }
}
