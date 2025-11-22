package com.fuzis.integrationbus.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;

@RefreshScope
@Component
@Getter
@EnableScheduling
public class SSOConfiguration {
    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.service}")
    private String service;

    private JwtDecoder jwtDecoder;

    @Scheduled(fixedRate = 10000) // 10 секунд
    public void refreshDecoder() {
        String jwkSetUri = this.getKeycloakUrl() + "/realms/" + this.getRealm() + "/protocol/openid-connect/certs";
        this.jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
