package com.example.demo.controller;

import cn.gzten.security.security.AuthContext;
import cn.gzten.security.security.AuthUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class TestSecurity {
    @GetMapping("/test-security")
    public AuthUser test(ServerHttpRequest request) {
        return AuthContext.getAuthUser(request);
    }
}
