package com.example.demo.controller;

import com.example.demo.data.entity.UserEntity;
import com.example.demo.data.repository.UserRepository;
import com.example.demo.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.nio.file.ProviderNotFoundException;

@RestController
@Slf4j
public class IndexController {
    @Resource
    UserRepository userRepo;

    private static final AppException NOT_FOUND_EXCEPTION = new AppException(400, "Not found the id!");
    @GetMapping("/users/{userId}")
    public Mono<UserEntity> getUser(@PathVariable("userId") Long userId) {
        return userRepo.findById(userId)
                .switchIfEmpty(Mono.error(NOT_FOUND_EXCEPTION));
    }

}
