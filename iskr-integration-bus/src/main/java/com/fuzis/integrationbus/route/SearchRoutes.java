package com.fuzis.integrationbus.route;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class SearchRoutes extends RouteBuilder {
    @Override
    public void configure() {
        errorHandler(defaultErrorHandler()
                .maximumRedeliveries(0)
                .retryAttemptedLogLevel(LoggingLevel.WARN));

        from("platform-http:/oapi/v1/search/query?httpMethodRestrict=POST")
                .routeId("search-query-route")
                .setHeader("X-Headers-Required", constant("Query"))
                .to("direct:check-params")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("X-Service", constant("Search"))
                .setHeader("X-Service-Request", simple("api/v1/search/query"))
                .process(exchange -> {
                    StringBuilder body = new StringBuilder();
                    String query = exchange.getIn().getHeader("Query", String.class);
                    if (query != null) {
                        body.append("query=").append(URLEncoder.encode(query, StandardCharsets.UTF_8));
                    }

                    String limit = exchange.getIn().getHeader("Limit", String.class);
                    if (limit != null) {
                        if (!body.isEmpty()) body.append("&");
                        body.append("limit=").append(URLEncoder.encode(limit, StandardCharsets.UTF_8));
                    }

                    String types = exchange.getIn().getHeader("Types", String.class);
                    if (types != null) {
                        if (!body.isEmpty()) body.append("&");
                        body.append("types=").append(URLEncoder.encode(types, StandardCharsets.UTF_8));
                    }
                    String genre = exchange.getIn().getHeader("Genre", String.class);
                    if (genre != null) {
                        if (!body.isEmpty()) body.append("&");
                        body.append("genreId=").append(URLEncoder.encode(genre, StandardCharsets.UTF_8));
                    }

                    exchange.getIn().setBody(body.toString());
                })
                .setHeader(Exchange.CONTENT_TYPE, constant("application/x-www-form-urlencoded"))
                .to("direct:sd-call-finalize");
    }
}
