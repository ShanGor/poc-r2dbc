package com.example.demo.controller;

import cn.gzten.security.security.AuthUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class TestUser {
    @GetMapping("/test")
    public AuthUser test(@RequestAttribute(AuthUser.AUTH_USER) AuthUser user) {
        return user;
    }
    @GetMapping("/test1")
    public AuthUser test1(@RequestAttribute(AuthUser.AUTH_USER) AuthUser user) {
        return user;
    }
}
