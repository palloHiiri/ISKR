package com.fuzis.integrationbus.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ExceptionProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(ExceptionProcessor.class);


    @Override
    public void process(Exchange exchange) throws Exception {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("message", exception != null ? exception.getMessage() : "Unknown error");
        errorResponse.put("errorType", exception != null ? exception.getClass().getSimpleName() : "Unknown");
        if (exception != null && exception.getCause() != null) {
            errorResponse.put("rootCause", exception.getCause().getMessage());
        }
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
        exchange.getIn().setBody(errorResponse);
    }
}
