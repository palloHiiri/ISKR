package com.fuzis.mailer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MailerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MailerApplication.class, args);
    }

}
