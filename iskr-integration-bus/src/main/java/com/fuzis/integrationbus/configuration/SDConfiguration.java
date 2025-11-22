package com.fuzis.integrationbus.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableScheduling
public class SDConfiguration
{
    private final DiscoveryClient consulDiscoveryClient;

    public SDConfiguration(@Autowired DiscoveryClient discoveryClient){
        this.consulDiscoveryClient = discoveryClient;
        this.discovery = new HashMap<>();
    }

    @Getter
    Map<String, List<ServiceInstance>> discovery;

    @Scheduled(fixedRate = 10000) // 10 секунд
    public void refreshServices() {
        List<String> services = consulDiscoveryClient.getServices();
        services.forEach(service -> {
            this.discovery.put(service, consulDiscoveryClient.getInstances(service));
        });
    }
}
