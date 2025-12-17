package com.fuzis.integrationbus.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MarshallProcessor implements Processor {
    private final Logger logger = LoggerFactory.getLogger(MarshallProcessor.class.getName());

    private final ObjectMapper objectMapper;

    public MarshallProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Object body = exchange.getIn().getBody();
        try {
            String jsonResult = objectMapper.writeValueAsString(body);
            exchange.getIn().setBody(jsonResult);
        } catch (Exception ignore) {
            String result = body.toString();
            exchange.getIn().setBody(result);
        }
    }
}
