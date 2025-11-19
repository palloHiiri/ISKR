package com.fuzis.integrationbus.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class BackendErrorProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(BackendErrorProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        Integer statusCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        String body = exchange.getIn().getBody(String.class);
        String userId = exchange.getIn().getHeader("X-User-ID", String.class);

        log.error("Service returned error for user {}: {} - {}", userId, statusCode, body);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Service returned error");
        errorResponse.put("code", statusCode != null ? statusCode : 500);
        errorResponse.put("details", body != null ? body : "Unknown error");
        errorResponse.put("userId", userId);
        errorResponse.put("timestamp", java.time.Instant.now().toString());

        exchange.getIn().setBody(errorResponse);
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");

        // Сохраняем оригинальный код ответа или устанавливаем 500
        if (statusCode == null) {
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
        }
    }
}
