package com.restartpoint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RestartPointApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestartPointApplication.class, args);
    }
}
