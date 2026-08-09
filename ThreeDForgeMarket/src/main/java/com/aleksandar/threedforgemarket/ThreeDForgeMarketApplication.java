package com.aleksandar.threedforgemarket;

import com.aleksandar.threedforgemarket.config.env.DotenvEnvironmentLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@ConfigurationPropertiesScan
public class ThreeDForgeMarketApplication {

    public static void main(String[] args) {
        DotenvEnvironmentLoader.load();
        SpringApplication.run(ThreeDForgeMarketApplication.class, args);
    }

}
