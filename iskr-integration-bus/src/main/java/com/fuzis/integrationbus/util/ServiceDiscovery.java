package com.fuzis.integrationbus.util;

import com.fuzis.integrationbus.configuration.SDConfiguration;
import com.fuzis.integrationbus.exception.ServiceDiscoveryFailed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ServiceDiscovery
{
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final SDConfiguration sdConfiguration;

    public ServiceDiscovery(@Autowired SDConfiguration sdConfiguration){
        this.sdConfiguration = sdConfiguration;
    }

    private final Map<String,Integer> round_robin_counter = new HashMap<>();

    private synchronized Integer getNextCounterVal(String service){
        if(!round_robin_counter.containsKey(service))round_robin_counter.put(service,0);
        if (round_robin_counter.get(service) > 1000000){
            round_robin_counter.put(service,0);
        }
        round_robin_counter.put(service,round_robin_counter.get(service) + 1);
        return round_robin_counter.get(service);
    }

    private String getServiceUrl(String service) throws ServiceDiscoveryFailed {
        List<ServiceInstance> list = sdConfiguration.getDiscovery().get(service);
        if (list != null && !list.isEmpty()) {
            return list.get(getNextCounterVal(service)%list.size()).getUri().toString();
        }
        log.error("No service instance found for service {}",service);
        throw new ServiceDiscoveryFailed("No service instances found, service: " + service);
    }

    public String getAccountsServiceUrl() throws ServiceDiscoveryFailed{
        return getServiceUrl("AccountsBackend");
    }

    public String getIntegrationUrl() throws ServiceDiscoveryFailed{
        return getServiceUrl("IntegrationBus");
    }

    public String getSearchUrl() throws ServiceDiscoveryFailed{
        return getServiceUrl("SearchService");
    }

    public String getBooksUrl() throws ServiceDiscoveryFailed{
        return getServiceUrl("BooksBackend");
    }

    public String getImagesUrl() throws ServiceDiscoveryFailed{
        return getServiceUrl("ImageService");
    }

}
