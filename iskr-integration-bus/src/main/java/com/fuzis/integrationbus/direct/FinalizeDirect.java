package com.fuzis.integrationbus.direct;

import com.fuzis.integrationbus.processor.EnrichProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FinalizeDirect extends RouteBuilder {

    private final EnrichProcessor enrichProcessor;

    public FinalizeDirect(EnrichProcessor enrichProcessor) {
        this.enrichProcessor = enrichProcessor;
    }

    @Override
    public void configure() throws Exception {
        from("direct:finalize-request")
                .process(enrichProcessor)
                .choice()
                    .when(header("X-Debug").isNotEqualTo("true"))
                        .removeHeader("X-Email-Verified")
                        .removeHeader("X-Realm-Roles")
                        .removeHeader("X-Client-Roles")
                        .removeHeader("X-Session-ID")
                        .removeHeader("X-Service-Request")
                        .removeHeader("X-Service")
                        .removeHeader("X-Email")
                        .removeHeader("X-Nickname")
                        .removeHeader("Authorization")
                        .removeHeader("X-Roles-Required")
                .end()
                .marshal().json()
                .end();
    }
}
