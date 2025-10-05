package com.library.ireaderbackend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 首页
@RestController
@RequestMapping("/api/test")
public class TestController {
    @GetMapping
    public String hello() {
        return "Hello iReader!";
    }
}
