package com.fuzis.integrationbus.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Data
@Builder
@Getter
public class UserInfo {
    private String subject;
    private String userId;
    private String nickname;
    private String login;
    private String email;
    private Boolean emailVerified;
    private List<String> realmRoles;
    private List<String> clientRoles;
    private Instant issuedAt;
    private Instant expiresAt;

    public boolean hasRole(String role) {
        return realmRoles.contains(role) || clientRoles.contains(role);
    }

    public boolean hasAnyRole(String... roles) {
        return Arrays.stream(roles)
                .anyMatch(role -> realmRoles.contains(role) || clientRoles.contains(role));
    }

    public boolean hasAllRoles(String... roles) {
        return Arrays.stream(roles)
                .allMatch(role -> realmRoles.contains(role) || clientRoles.contains(role));
    }
}
