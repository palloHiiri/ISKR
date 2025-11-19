package com.fuzis.integrationbus.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@RefreshScope
@Component
public class SSOConfiguration {
    @Value("${keycloak.auth-server-url}")
    public String keycloakUrl;

    @Value("${keycloak.realm}")
    public String realm;

    @Value("${keycloak.bearer-only}")
    public Boolean bearer_only;

    @Value("${keycloak.service}")
    public String service;
}
