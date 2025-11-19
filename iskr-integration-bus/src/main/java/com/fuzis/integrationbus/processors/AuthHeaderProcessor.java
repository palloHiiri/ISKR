package com.fuzis.integrationbus.processors;

import com.fuzis.integrationbus.auth.JwtValidator;
import com.fuzis.integrationbus.exception.AuthenticationException;
import com.fuzis.integrationbus.exception.AuthorizationException;
import com.fuzis.integrationbus.model.UserInfo;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class AuthHeaderProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(AuthHeaderProcessor.class);

    private final JwtValidator jwtValidator;


    public AuthHeaderProcessor(@Autowired JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String authHeader = exchange.getIn().getHeader("Authorization", String.class);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AuthenticationException("Missing or invalid authorization header");
        }

        String token = authHeader.substring(7);
        UserInfo userInfo = jwtValidator.extractUserInfo(token);

        // Добавляем информацию в заголовки
        exchange.getIn().setHeader("X-User", userInfo.getSubject());
        exchange.getIn().setHeader("X-User-ID", userInfo.getUserId());
        exchange.getIn().setHeader("X-Login", userInfo.getLogin());
        exchange.getIn().setHeader("X-Email", userInfo.getEmail());
        exchange.getIn().setHeader("X-Email-Verified", userInfo.getEmailVerified());
        exchange.getIn().setHeader("X-Nickname", userInfo.getNickname());
        exchange.getIn().setHeader("X-User-Roles", String.join(",", userInfo.getRealmRoles()));
        exchange.getIn().setHeader("X-Client-Roles", String.join(",", userInfo.getClientRoles()));
        exchange.getIn().setHeader("X-Authenticated", "true");

        checkPermissions(exchange, userInfo);
    }

    private void checkPermissions(Exchange exchange, UserInfo userInfo) throws Exception {
        String path = exchange.getIn().getHeader("CamelHttpPath", String.class);
        String method = exchange.getIn().getHeader("CamelHttpMethod", String.class);
        String roles = exchange.getIn().getHeader("X-Roles-Required", String.class);

        String[] roleList = roles.split(",");

        if(!userInfo.hasAllRoles(roleList)){
            throw new AuthorizationException("No access to this resource");
        }

        log.debug("User {} authorized for {} {}", userInfo.getLogin(), method, path);
    }
}