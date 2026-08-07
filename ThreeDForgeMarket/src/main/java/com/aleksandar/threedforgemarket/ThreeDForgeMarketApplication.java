package com.aleksandar.threedforgemarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ThreeDForgeMarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThreeDForgeMarketApplication.class, args);
    }

}
