package com.fuzis.integrationbus.route;

import com.fuzis.integrationbus.exception.AuthenticationException;
import com.fuzis.integrationbus.processor.AuthHeaderProcessor;
import com.fuzis.integrationbus.processor.BackendErrorProcessor;
import com.fuzis.integrationbus.processor.EnrichProcessor;
import com.fuzis.integrationbus.processor.JsonBodyValidationProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AccountsUserRoutes extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(AccountsUserRoutes.class);

    private final AuthHeaderProcessor authHeaderProcessor;

    private final EnrichProcessor enrichProcessor;

    private final JsonBodyValidationProcessor jsonBodyValidationProcessor;

    private final BackendErrorProcessor backendErrorProcessor;

    public AccountsUserRoutes(
            @Autowired EnrichProcessor enrichProcessor,
            @Autowired AuthHeaderProcessor authHeaderProcessor,
            @Autowired JsonBodyValidationProcessor jsonBodyValidationProcessor,
            @Autowired BackendErrorProcessor backendErrorProcessor) {
        this.authHeaderProcessor = authHeaderProcessor;
        this.enrichProcessor = enrichProcessor;
        this.jsonBodyValidationProcessor = jsonBodyValidationProcessor;
        this.backendErrorProcessor = backendErrorProcessor;
    }

    @Override
    public void configure() {

        errorHandler(defaultErrorHandler()
                .maximumRedeliveries(0)
                .retryAttemptedLogLevel(LoggingLevel.WARN));

        from("platform-http:/oapi/v1/accounts/user?httpMethodRestrict=GET")
                .routeId("accounts-user-get-route")
                .setHeader("X-Roles-Required", constant("profile-watch"))
                .to("direct:auth")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader("X-Service", constant("Accounts"))
                .setHeader("X-Service-Request", simple("api/v1/accounts/user/${header.X-User-ID}"))
                .to("direct:sd-call-finalize");

        from("platform-http:/oapi/v1/accounts/user?httpMethodRestrict=PUT")
                .routeId("accounts-user-put-route")
                .setHeader("X-Roles-Required", constant("profile-watch,profile-change"))
                .to("direct:auth")
                .setHeader(Exchange.HTTP_METHOD, constant("PUT"))
                .setHeader("X-Service", constant("Accounts"))
                .setHeader("X-Service-Request", simple("api/v1/accounts/user/${header.X-User-ID}"))
                .to("direct:sd-call-finalize");

        from("platform-http:/oapi/v1/accounts/user?httpMethodRestrict=OPTIONS")
                .routeId("accounts-user-options-route")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                .setHeader("Access-Control-Allow-Origin", constant("*"))
                .setHeader("Access-Control-Allow-Methods", constant("GET, PUT, OPTIONS"))
                .setHeader("Access-Control-Allow-Headers", constant("Content-Type, Authorization"))
                .setBody(constant(""));
    }
}