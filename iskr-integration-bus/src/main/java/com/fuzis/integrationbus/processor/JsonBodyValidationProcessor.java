package com.fuzis.integrationbus.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class JsonBodyValidationProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(JsonBodyValidationProcessor.class);

    @Override
    public void process(Exchange exchange) {
        String body = exchange.getIn().getBody(String.class);
        if (body == null || body.trim().isEmpty()) {
            throw new IllegalArgumentException("Request body cannot be empty");
        }
        try {
            exchange.getContext().getTypeConverter().convertTo(Map.class, exchange, body);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON in request body");
        }

        log.debug("Request body validation passed for user: {}",
                exchange.getIn().getHeader("X-Login"));
    }
}
