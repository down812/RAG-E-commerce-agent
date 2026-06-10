package com.ecommerceserver;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.ecommerceserver.mapper")
public class ECommerceServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ECommerceServerApplication.class, args);
    }

}
