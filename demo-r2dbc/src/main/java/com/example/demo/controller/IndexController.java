package com.example.demo.controller;

import com.example.demo.data.entity.UserEntity;
import com.example.demo.data.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

@RestController
public class IndexController {

    @Resource
    private UserRepository userRepo;
    @GetMapping("/users/{userId}")
    public Mono<UserEntity> getUser(@PathVariable("userId") Long userId) {
        return userRepo.findById(userId);
    }

}
