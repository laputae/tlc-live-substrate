package com.tlc.live.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "com.tlc.live")
@ConfigurationPropertiesScan
public class SubstrateGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubstrateGatewayApplication.class, args);
    }
}
