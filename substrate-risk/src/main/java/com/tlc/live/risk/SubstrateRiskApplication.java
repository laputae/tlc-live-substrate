package com.tlc.live.risk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "com.tlc.live")
@ConfigurationPropertiesScan
public class SubstrateRiskApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubstrateRiskApplication.class, args);
    }
}
