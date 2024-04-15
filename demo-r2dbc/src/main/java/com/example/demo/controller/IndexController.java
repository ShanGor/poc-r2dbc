package com.example.demo.controller;

import com.example.demo.data.entity.UserEntity;
import com.example.demo.data.repository.UserRepository;
import com.example.demo.exception.AppException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.annotation.Resource;

@RestController
@Slf4j
public class IndexController {
    @Resource
    UserRepository userRepo;

    ObjectMapper om = new ObjectMapper();

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

    @Resource
    R2dbcEntityTemplate template;

    @GetMapping("/test-sql")
    public Flux testSql() {
        // the element is a mutable map, so we do not need to copy it.
        return template.getDatabaseClient().sql("select * from test").fetch().all().map(o -> {
            o.entrySet().forEach(e -> {
                if (e.getValue() instanceof io.r2dbc.postgresql.codec.Json) {
                    String value = ((Json) e.getValue()).asString();
                    try {
                        if (value.startsWith("{")){
                            o.put(e.getKey(), om.readValue(value, java.util.Map.class));
                        } else if (value.startsWith("[")){
                            o.put(e.getKey(), om.readValue(value, java.util.List.class));
                        } else {
                            o.put(e.getKey(), value);
                        }
                    } catch (JsonProcessingException ex) {
                        o.put(e.getKey(), value);
                    }
                }
            });
            return o;
        });
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
