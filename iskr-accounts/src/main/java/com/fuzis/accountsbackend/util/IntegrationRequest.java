package com.fuzis.accountsbackend.util;

import com.fuzis.accountsbackend.configuration.IntegrationConfiguration;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.Map;

@Component
public class IntegrationRequest
{
    private final IntegrationConfiguration integrationConfiguration;

    public IntegrationRequest(IntegrationConfiguration integrationConfiguration){
        this.integrationConfiguration = integrationConfiguration;
    }

    public ResponseEntity<Map> sendPostRequestIntegration(String address, MultiValueMap<String, String> body) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://" + integrationConfiguration.getIntegrationHost()
                        + ":" + integrationConfiguration.getIntegrationPort()
                        + "/oapi-inner/"
                        + address,
                request,
                Map.class
        );
        return response;
    }
}
