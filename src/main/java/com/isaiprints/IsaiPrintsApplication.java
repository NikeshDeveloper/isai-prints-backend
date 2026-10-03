package com.isaiprints;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IsaiPrintsApplication {
    public static void main(String[] args) {
        SpringApplication.run(IsaiPrintsApplication.class, args);
    }
}
