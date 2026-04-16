package com.example.security.client.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ClientController {

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal OAuth2User oauth2User, Model model) {
        model.addAttribute("user", oauth2User);
        return "dashboard";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal OAuth2User oauth2User, Model model) {
        model.addAttribute("user", oauth2User);
        return "profile";
    }
}
