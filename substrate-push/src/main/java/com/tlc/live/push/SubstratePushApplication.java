package com.tlc.live.push;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.tlc.live")
public class SubstratePushApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubstratePushApplication.class, args);
    }
}
