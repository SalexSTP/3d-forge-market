package com.aleksandar.customprintservice;

import com.aleksandar.customprintservice.config.env.DotenvEnvironmentLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CustomPrintServiceApplication {

    public static void main(String[] args) {
        DotenvEnvironmentLoader.load();
        SpringApplication.run(CustomPrintServiceApplication.class, args);
    }

}
