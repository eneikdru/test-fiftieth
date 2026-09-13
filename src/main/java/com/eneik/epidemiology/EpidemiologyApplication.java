package com.eneik.epidemiology;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EpidemiologyApplication {
    public static void main(String[] args) {
        SpringApplication.run(EpidemiologyApplication.class, args);
    }
}
