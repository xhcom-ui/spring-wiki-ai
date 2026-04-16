package org.wiki.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
//你的判断完全正确。thread.isVirtual()报错，根本原因在于 JDK 版本，而非 Spring Boot 3.2.0。
@RestController
public class TestController {

//    @GetMapping("/test")
//    public String test() {
//        Thread thread = Thread.currentThread();
//        return String.format("线程名称: %s, 是否为虚拟线程: %s",
//                            thread.getName(),
//                            thread.isVirtual());
//    }
}