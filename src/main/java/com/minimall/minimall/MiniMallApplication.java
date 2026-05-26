package com.minimall.minimall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class
MiniMallApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniMallApplication.class, args);
    }

}
