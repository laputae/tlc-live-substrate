package com.tlc.live.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.tlc.live")
public class SubstrateAuditApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubstrateAuditApplication.class, args);
    }
}
