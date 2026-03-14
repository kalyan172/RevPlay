package com.revplay.musicplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class RevplayApplication {

    public static void main(String[] args) {
        SpringApplication.run(RevplayApplication.class, args);
    }
}