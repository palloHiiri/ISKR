package com.fuzis.integrationbus.direct;

import com.fuzis.integrationbus.exception.AuthenticationException;
import com.fuzis.integrationbus.exception.AuthorizationException;
import com.fuzis.integrationbus.processor.AuthHeaderProcessor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthDirect extends RouteBuilder {

    private final AuthHeaderProcessor authHeaderProcessor;

    private AuthDirect(@Autowired  AuthHeaderProcessor authHeaderProcessor) {
        this.authHeaderProcessor = authHeaderProcessor;
    }

    @Override
    public void configure() throws Exception {
        from("direct:auth")
            .onException(AuthenticationException.class)
                .handled(true)
                .to("direct:auth-error-handler")
            .end()
            .onException(AuthorizationException.class)
                .handled(true)
                .to("direct:auth-error-handler")
            .end()
            .log("Parsing auth data")
            .process(authHeaderProcessor)
            .log("User authorized, user id: ${header.X-User-ID}")
            .end();
    }
}
