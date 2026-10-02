package com.tlc.live.toggle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "com.tlc.live")
@ConfigurationPropertiesScan
public class SubstrateToggleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubstrateToggleApplication.class, args);
    }
}
