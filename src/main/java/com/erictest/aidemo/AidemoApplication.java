package com.erictest.aidemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.erictest.aidemo.mapper")
public class AidemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AidemoApplication.class, args);
    }

}
