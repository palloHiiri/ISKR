package com.fuzis.integrationbus.direct;

import com.fuzis.integrationbus.processor.ExceptionProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthErrorHandlerDirect extends RouteBuilder {


    private final ExceptionProcessor exceptionProcessor;

    public AuthErrorHandlerDirect(@Autowired ExceptionProcessor exceptionProcessor) {
        this.exceptionProcessor = exceptionProcessor;
    }

    @Override
    public void configure(){
        from("direct:auth-error-handler")
            .log("Auth failed")
            .process(exceptionProcessor)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(401))
            .to("direct:finalize-request")
            .end();
    }
}
