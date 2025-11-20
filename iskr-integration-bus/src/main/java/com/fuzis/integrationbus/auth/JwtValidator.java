package com.fuzis.integrationbus.auth;

import com.fuzis.integrationbus.configuration.SSOConfiguration;
import com.fuzis.integrationbus.exception.AuthenticationException;
import com.fuzis.integrationbus.model.UserInfo;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class JwtValidator {
    private static final Logger log = LoggerFactory.getLogger(JwtValidator.class);

    private final SSOConfiguration ssoConfiguration;

    public JwtValidator(@Autowired SSOConfiguration ssoConfiguration) {
        this.ssoConfiguration = ssoConfiguration;
    }

    private JwtDecoder jwtDecoder;


    private String jwkSetUri;

    @PostConstruct
    public void init() {
        String jwkSetUri = ssoConfiguration.getKeycloakUrl() + "/realms/" + ssoConfiguration.getRealm() + "/protocol/openid-connect/certs";
        this.jwkSetUri = jwkSetUri;
        this.jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    public Jwt validateToken(String token) throws AuthenticationException{
        log.debug("Formed url: {}", jwkSetUri);
        try {
            return jwtDecoder.decode(token);
        } catch (Exception e) {
            throw new AuthenticationException("Invalid JWT token", e);
        }
    }

    public UserInfo extractUserInfo(String token) throws AuthenticationException {
        Jwt jwt = validateToken(token);

        return UserInfo.builder()
                .subject(jwt.getSubject())
                .userId(jwt.getClaimAsString("given_name"))
                .nickname(jwt.getClaimAsString("family_name"))
                .login(jwt.getClaimAsString("username"))
                .email(jwt.getClaimAsString("email"))
                .emailVerified(jwt.getClaimAsBoolean("email_verified"))
                .realmRoles(extractRealmRoles(jwt))
                .clientRoles(extractClientRoles(jwt))
                .issuedAt(jwt.getIssuedAt())
                .expiresAt(jwt.getExpiresAt())
                .build();
    }

    private List<String> extractRealmRoles(Jwt jwt) throws AuthenticationException{
        var realm_access = jwt.getClaimAsMap("realm_access");
        Object RealmRoles =  realm_access.get("roles");
        if (RealmRoles instanceof List){
            return (List<String>) RealmRoles;
        }
        throw new AuthenticationException("Realm roles not found");
    }

    private List<String> extractClientRoles(Jwt jwt) throws AuthenticationException{
        var resource_access = jwt.getClaimAsMap("resource_access");
        Object clients = resource_access.get(ssoConfiguration.getService());
        if (clients instanceof Map){
            Object ClientRoles =  ((Map<?, ?>) clients).get("roles");
            if (ClientRoles instanceof List){
                return (List<String>) ClientRoles;
            }
        }
        throw new AuthenticationException("Client roles not found");

    }
}