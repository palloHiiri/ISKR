package com.fuzis.integrationbus.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class EnrichProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(EnrichProcessor.class);

    private UnmarshallProcessor unmarshallProcessor;

    private EnrichProcessor(@Autowired UnmarshallProcessor unmarshallProcessor) {
        this.unmarshallProcessor = unmarshallProcessor;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        //unmarshallProcessor.process(exchange);
        try {
            Object currentBody = exchange.getIn().getBody();
            Boolean noMeta = exchange.getIn().getHeader("X-No-Meta", Boolean.class);
            if (noMeta != null && noMeta) {
                exchange.getIn().setBody(currentBody);
                exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
                return;
            }
            String userId = exchange.getIn().getHeader("X-User-Id", String.class);
            if(userId == null)userId = "None";
            Map<String, Object> enrichedResponse = new HashMap<>();
            enrichedResponse.put("data", currentBody);
            enrichedResponse.put("meta", Map.of(
                    "processedBy", "integration-bus",
                    "userId", userId,
                    "timestamp", ZonedDateTime.now().toString()
            ));

            exchange.getIn().setBody(enrichedResponse);
            exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");

        } catch (Exception e) {
            log.warn("Could not enrich response, returning original", e);
        }
    }
}
