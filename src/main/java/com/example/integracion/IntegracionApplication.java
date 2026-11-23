package com.example.integracion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IntegracionApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntegracionApplication.class, args);
    }
}