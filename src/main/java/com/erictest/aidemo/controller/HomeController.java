package com.erictest.aidemo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 首頁控制器
 */
@Controller
public class HomeController {

    /**
     * 首頁
     */
    @GetMapping("/")
    public String home() {
        return "index";
    }
}
