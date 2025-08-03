package com.example.jibmusil;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class JibmusilApplication {
    public static void main(String[] args) {
        SpringApplication.run(JibmusilApplication.class, args);
    }
}