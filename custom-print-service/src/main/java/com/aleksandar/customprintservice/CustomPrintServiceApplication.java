package com.aleksandar.customprintservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CustomPrintServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomPrintServiceApplication.class, args);
    }

}
