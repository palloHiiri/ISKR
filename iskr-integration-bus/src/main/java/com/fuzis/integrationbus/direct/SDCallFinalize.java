package com.fuzis.integrationbus.direct;

import com.fuzis.integrationbus.processor.BackendErrorProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SDCallFinalize extends RouteBuilder {

    private final BackendErrorProcessor backendErrorProcessor;

    public SDCallFinalize(@Autowired BackendErrorProcessor backendErrorProcessor) {
        this.backendErrorProcessor = backendErrorProcessor;
    }

    @Override
    public void configure() throws Exception {
        from("direct:sd-call-finalize")
                .to("direct:sd-call")
                .unmarshal().json(JsonLibrary.Jackson, Map.class)
                .filter(header(Exchange.HTTP_RESPONSE_CODE).isEqualTo(200))
                    .log("Backend Call Successful")
                .end()
                .filter(header(Exchange.HTTP_RESPONSE_CODE).isNotEqualTo(200))
                    .log("Backend Call Unsuccessful, error: ${header.CamelHttpResponseCode}")
                    .process(backendErrorProcessor)
                .end()
                .to("direct:finalize-request")
                .end();
    }
}
