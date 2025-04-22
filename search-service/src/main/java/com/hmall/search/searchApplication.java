package com.hmall.search;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.hmall.search.mapper")
@SpringBootApplication
public class searchApplication {
    public static void main(String[] args) {
        SpringApplication.run(searchApplication.class, args);
    }
}