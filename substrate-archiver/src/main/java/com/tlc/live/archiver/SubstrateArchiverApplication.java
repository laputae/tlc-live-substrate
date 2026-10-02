package com.tlc.live.archiver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.tlc.live")
@EnableScheduling
@ConfigurationPropertiesScan
public class SubstrateArchiverApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubstrateArchiverApplication.class, args);
    }
}
