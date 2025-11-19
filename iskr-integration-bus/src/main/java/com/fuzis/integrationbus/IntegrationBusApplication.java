package com.fuzis.integrationbus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class IntegrationBusApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntegrationBusApplication.class, args);
    }

}
