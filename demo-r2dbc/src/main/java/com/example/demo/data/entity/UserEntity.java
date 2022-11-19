package com.example.demo.data.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.sql.Timestamp;

@Data
@Table(name = "test_r2dbc")
public class UserEntity {
    @Id
    private long id;
    private String username;
    private Timestamp lastUpdateTime;
}
