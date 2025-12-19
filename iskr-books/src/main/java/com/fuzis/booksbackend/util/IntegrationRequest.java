package com.fuzis.booksbackend.util;

import com.fuzis.booksbackend.configuration.IntegrationConfiguration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.io.IOException;
import java.util.Map;

@Component
public class IntegrationRequest {
    private final IntegrationConfiguration integrationConfiguration;
    private final RestTemplate restTemplate;

    public IntegrationRequest(IntegrationConfiguration integrationConfiguration) {
        this.integrationConfiguration = integrationConfiguration;
        this.restTemplate = createRestTemplate();
    }

    private RestTemplate createRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return false;
            }

            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
            }
        });

        return restTemplate;
    }

    public ResponseEntity<Map> sendPostRequestIntegration(String address, MultiValueMap<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        String url = "http://" + integrationConfiguration.getIntegrationHost()
                + ":" + integrationConfiguration.getIntegrationPort()
                + "/oapi-inner/"
                + address;

        return restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                Map.class
        );
    }
}
