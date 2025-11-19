package com.fuzis.integrationbus.route;

import com.fuzis.integrationbus.exception.AuthenticationException;
import com.fuzis.integrationbus.processors.AuthHeaderProcessor;
import com.fuzis.integrationbus.processors.BackendErrorProcessor;
import com.fuzis.integrationbus.processors.EnrichProcessor;
import com.fuzis.integrationbus.processors.JsonBodyValidationProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
                .onException(AuthenticationException.class)
                    .log("Ошибка: ${exception.message}")
                    .handled(true)
                    .to("direct:error-authentication-handler")
                .end()
                .log("GET request for user profile")
                .setHeader("X-Roles-Required", constant("profile-watch"))
                .process(authHeaderProcessor)
                .log("User authorized for GET: ${header.X-Login}")
                .removeHeader("Authorization")
                .removeHeader("X-Roles-Required")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .log("Forwarding GET to accounts service for user id: ${header.X-User-ID}")
                //.process(this::prepareServiceCall)
                .loadBalance().roundRobin()
                .to("consul:http://AccountsBackend")
                .end()
                //.toD("${header.targetUrl}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .log("Received GET response from accounts service: ${body}")
                .choice()
                .when(header(Exchange.HTTP_RESPONSE_CODE).isEqualTo(200))
                .log("Successfully retrieved user profile")
                .process(enrichProcessor)
                .otherwise()
                .log("Accounts service returned error: ${header.CamelHttpResponseCode}")
                .process(backendErrorProcessor)
                .end();

        from("platform-http:/oapi/v1/accounts/user?httpMethodRestrict=PUT")
                .routeId("accounts-user-put-route")
                .log("PUT request to update user profile")
                .setHeader("X-Roles-Required", constant("profile-change,profile-watch"))
                .process(authHeaderProcessor)
                .log("User authorized for PUT: ${header.X-Login}")
                .process(jsonBodyValidationProcessor)
                .removeHeader("Authorization")
                .removeHeader("X-Roles-Required")
                .setHeader(Exchange.HTTP_METHOD, constant("PUT"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .log("Forwarding PUT to accounts service for user id: ${header.X-User-ID}")
                .loadBalance().roundRobin()
                .to("consul:http://AccountsBackend")
                .end()
                .toD("${header.targetUrl}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .log("📤 Received PUT response from accounts service: ${body}")
                .choice()
                .when(header(Exchange.HTTP_RESPONSE_CODE).isEqualTo(200))
                .log("Successfully updated user profile")
                .process(enrichProcessor)
                .otherwise()
                .log("Accounts service returned error: ${header.CamelHttpResponseCode}")
                .process(backendErrorProcessor)
                .end();

        from("platform-http:/oapi/v1/accounts/user?httpMethodRestrict=OPTIONS")
                .routeId("accounts-user-options-route")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                .setHeader("Access-Control-Allow-Origin", constant("*"))
                .setHeader("Access-Control-Allow-Methods", constant("GET, PUT, OPTIONS"))
                .setHeader("Access-Control-Allow-Headers", constant("Content-Type, Authorization"))
                .setBody(constant(""));
    }
}