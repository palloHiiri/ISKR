package com.fuzis.integrationbus.route;

import com.fuzis.integrationbus.exception.ServiceFall;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class BooksCommonRoutes extends RouteBuilder {
    @Override
    public void configure() {
        errorHandler(defaultErrorHandler()
                .maximumRedeliveries(0)
                .retryAttemptedLogLevel(LoggingLevel.WARN));

        from("platform-http:/oapi/v1/popular/collections?httpMethodRestrict=GET")
                .routeId("books-popular-collections-route")
                .onException(ServiceFall.class)
                    .handled(true)
                    .to("direct:service-error-handler")
                .end()
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader("X-Service", constant("Books"))
                .setHeader("X-Service-Request", simple("api/v1/popular/collections"))
                .to("direct:sd-call-finalize");

        from("platform-http:/oapi/v1/popular/users?httpMethodRestrict=GET")
                .routeId("books-popular-users-route")
                .onException(ServiceFall.class)
                    .handled(true)
                    .to("direct:service-error-handler")
                .end()
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader("X-Service", constant("Books"))
                .setHeader("X-Service-Request", simple("api/v1/popular/users"))
                .to("direct:sd-call-finalize");

        from("platform-http:/oapi/v1/popular/books?httpMethodRestrict=GET")
                .routeId("books-popular-books-route")
                .onException(ServiceFall.class)
                    .handled(true)
                    .to("direct:service-error-handler")
                .end()
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader("X-Service", constant("Books"))
                .setHeader("X-Service-Request", simple("api/v1/popular/books"))
                .to("direct:sd-call-finalize");
    }
}

