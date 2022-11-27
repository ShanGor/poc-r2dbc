package com.example.demo.controller;

import com.example.demo.data.entity.UserEntity;
import com.example.demo.data.repository.UserRepository;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@RestController
public class MainController {
    @Resource
    private UserRepository userRepo;
    @GetMapping("/users/{userId}")
    public Optional<UserEntity> getUser(@PathVariable("userId") Long userId) {
        return userRepo.findById(userId);
    }

    @GetMapping("/users/random")
    public UserEntity createRandomUser() {
        var user = new UserEntity();
        user.setUsername(UUID.randomUUID().toString());
        user.setLastUpdateTime(new Timestamp(System.currentTimeMillis()));
        return userRepo.save(user);
    }
    @Resource
    HikariDataSource dataSource;
    @GetMapping("/users/random/{count}")
    public int[] createRandomUsers(@PathVariable("count") int count) {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            var stmt = conn.prepareStatement("insert into test_r2dbc(username) values (?)");
            for (int i=0; i<count; i++) {
                stmt.setString(1, UUID.randomUUID().toString());
                stmt.addBatch();
            }
            var res =  stmt.executeBatch();

            conn.commit();
            return res;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null)
                dataSource.evictConnection(conn);
        }
    }
}
