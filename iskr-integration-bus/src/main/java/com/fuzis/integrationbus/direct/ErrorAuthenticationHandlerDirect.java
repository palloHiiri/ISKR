package com.fuzis.integrationbus.direct;

import com.fuzis.integrationbus.processors.EnrichProcessor;
import com.fuzis.integrationbus.processors.ExceptionProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ErrorAuthenticationHandlerDirect extends RouteBuilder {

    private final EnrichProcessor enrichProcessor;

    private final ExceptionProcessor exceptionProcessor;

    public ErrorAuthenticationHandlerDirect( @Autowired EnrichProcessor enrichProcessor,  @Autowired ExceptionProcessor exceptionProcessor) {
        this.enrichProcessor = enrichProcessor;
        this.exceptionProcessor = exceptionProcessor;
    }

    @Override
    public void configure(){
        from("direct:error-authentication-handler")
            .log("Auth failed")
            .process(exceptionProcessor)
            .process(enrichProcessor)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(401))
            .marshal().json()
            .end();
    }
}
