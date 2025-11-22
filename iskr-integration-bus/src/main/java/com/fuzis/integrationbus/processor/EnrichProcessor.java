package com.fuzis.integrationbus.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class EnrichProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(EnrichProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        try {
            Map currentBody = exchange.getIn().getBody(Map.class);
            String userId = exchange.getIn().getHeader("X-User-Id", String.class);
            if(userId == null)userId = "None";


            Map<String, Object> enrichedResponse = new HashMap<>();
            enrichedResponse.put("data", currentBody);
            enrichedResponse.put("meta", Map.of(
                    "processedBy", "integration-bus",
                    "userId", userId,
                    "timestamp", java.time.Instant.now().toString()
            ));

            exchange.getIn().setBody(enrichedResponse);
            exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");

        } catch (Exception e) {
            log.warn("Could not enrich response, returning original", e);
        }
    }
}
