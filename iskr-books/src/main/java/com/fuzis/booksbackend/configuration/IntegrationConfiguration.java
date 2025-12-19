package com.fuzis.booksbackend.configuration;


import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class IntegrationConfiguration {
    @Value("${integration.host}")
    private String integrationHost;

    @Value("${integration.port}")
    private String integrationPort;
}
