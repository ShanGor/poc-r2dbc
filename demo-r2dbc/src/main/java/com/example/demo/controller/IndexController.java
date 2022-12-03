package com.example.demo.controller;

import com.example.demo.data.entity.UserEntity;
import com.example.demo.data.repository.UserRepository;
import com.example.demo.exception.AppException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.annotation.Resource;
import java.nio.file.ProviderNotFoundException;

@RestController
@Slf4j
public class IndexController {
    @Resource
    UserRepository userRepo;

    private static final AppException NOT_FOUND_EXCEPTION = new AppException(400, "Not found the id!");
    @GetMapping("/users/{userId}")
    public Mono<UserEntity> getUser(@PathVariable("userId") Long userId) {
//        log.info("/users/{}", userId);
        return userRepo.findById(userId)
                .switchIfEmpty(Mono.error(NOT_FOUND_EXCEPTION));
    }

    @PostMapping("/test")
    public String testPost(@RequestBody Hello hello, ServerHttpRequest request) {
        request.getHeaders().forEach((k, v) -> {
            System.out.printf("%s - %s %n", k, v.get(0));
        });
        return "Hello, %s".formatted(hello.message);
    }

    @Data
    static class Hello {
        private String message;
    }

    @GetMapping("/twice/{number}")
    public long twice(@PathVariable("number") long number) {
        return number * 2;
    }
}
