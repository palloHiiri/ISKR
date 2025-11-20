package com.fuzis.integrationbus.util;

import com.fuzis.integrationbus.exception.ServiceDiscoveryFailed;
import com.fuzis.integrationbus.processors.AuthHeaderProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;

@Component
public class ServiceDiscovery
{
    private final DiscoveryClient discoveryClient;

    public ServiceDiscovery(@Autowired DiscoveryClient discoveryClient){
        this.discoveryClient = discoveryClient;
    }

    private Integer round_robin_counter = 0;

    private synchronized Integer getNextCounterVal(){
        if (round_robin_counter > 1000000){
            round_robin_counter = 0;
        }
        return round_robin_counter++;
    }

    private String getServiceUrl(String service) throws ServiceDiscoveryFailed {
        List<ServiceInstance> list = discoveryClient.getInstances(service);
        if (list != null && !list.isEmpty()) {
            return list.get(getNextCounterVal()%list.size()).getUri().toString();
        }
        throw new ServiceDiscoveryFailed("No service instances found, service: " + service);
    }

    public String getAccountsServiceUrl() throws ServiceDiscoveryFailed{
        return getServiceUrl("AccountsBackend");
    }

}
