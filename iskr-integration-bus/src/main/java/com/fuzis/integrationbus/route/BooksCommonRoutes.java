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

        from("platform-http:/oapi/v1/user?httpMethodRestrict=GET")
                .routeId("user-books-route")
                .onException(ServiceFall.class)
                    .handled(true)
                    .to("direct:service-error-handler")
                .end()
                .setHeader("X-Headers-Required", constant("userId"))
                .to("direct:check-params")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader("X-Service", constant("Books"))
                .setHeader("X-Service-Request", simple("api/v1/users/${header.userId}"))
                .to("direct:sd-call-finalize");

        from("platform-http:/oapi/v1/user/subscribers?httpMethodRestrict=GET")
                .routeId("user-books-subscribers-route")
                .onException(ServiceFall.class)
                    .handled(true)
                    .to("direct:service-error-handler")
                .end()
                .setHeader("X-Headers-Required", constant("userId"))
                .to("direct:check-params")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader("X-Service", constant("Books"))
                .setHeader("X-Service-Request", simple("api/v1/users/${header.userId}/subscribers"))
                .to("direct:sd-call-finalize");

        from("platform-http:/oapi/v1/user/subscriptions?httpMethodRestrict=GET")
                .routeId("user-books-subscriptions-route")
                .onException(ServiceFall.class)
                    .handled(true)
                    .to("direct:service-error-handler")
                .end()
                .setHeader("X-Headers-Required", constant("userId"))
                .to("direct:check-params")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader("X-Service", constant("Books"))
                .setHeader("X-Service-Request", simple("api/v1/users/${header.userId}/subscriptions"))
                .to("direct:sd-call-finalize");

        from("platform-http:/oapi/v1/user/collections?httpMethodRestrict=GET")
                .routeId("user-books-collections-route")
                .onException(ServiceFall.class)
                .handled(true)
                .to("direct:service-error-handler")
                .end()
                .setHeader("X-Headers-Required", constant("userId"))
                .to("direct:check-params")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader("X-Service", constant("Books"))
                .setHeader("X-Service-Request", simple("api/v1/users/${header.userId}/collections"))
                .to("direct:sd-call-finalize");

        from("platform-http:/oapi/v1/collections?httpMethodRestrict=GET")
                .routeId("user-books-collection-route")
                .onException(ServiceFall.class)
                .handled(true)
                .to("direct:service-error-handler")
                .end()
                .setHeader("X-Headers-Required", constant("collectionId"))
                .to("direct:check-params")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader("X-Service", constant("Books"))
                .setHeader("X-Service-Request", simple("api/v1/collections/${header.collectionId}"))
                .to("direct:sd-call-finalize");

        from("platform-http:/oapi/v1/collections/books?httpMethodRestrict=GET")
                .routeId("user-books-collections-books-route")
                .onException(ServiceFall.class)
                .handled(true)
                .to("direct:service-error-handler")
                .end()
                .setHeader("X-Headers-Required", constant("collectionId"))
                .to("direct:check-params")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader("X-Service", constant("Books"))
                .setHeader("X-Service-Request", simple("api/v1/collections/${header.collectionId}/books"))
                .to("direct:sd-call-finalize");

        from("platform-http:/oapi/v1/collections/books/auth?httpMethodRestrict=GET")
                .routeId("user-books-collections-books-auth-route")
                .onException(ServiceFall.class)
                .handled(true)
                .to("direct:service-error-handler")
                .end()
                .setHeader("X-Headers-Required", constant("collectionId"))
                .to("direct:check-params")
                .setHeader("X-Roles-Required", constant(""))
                .to("direct:auth")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader("X-Service", constant("Books"))
                .setHeader("userId", simple("${header.X-User-ID}"))
                .setHeader("X-Service-Request", simple("api/v1/collections/${header.collectionId}/books"))
                .to("direct:sd-call-finalize");

        from("platform-http:/oapi/v1/collection/auth?httpMethodRestrict=GET")
                .routeId("user-books-collection-auth-route")
                .onException(ServiceFall.class)
                .handled(true)
                .to("direct:service-error-handler")
                .end()
                .setHeader("X-Headers-Required", constant("collectionId"))
                .to("direct:check-params")
                .setHeader("X-Roles-Required", constant(""))
                .to("direct:auth")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader("X-Service", constant("Books"))
                .setHeader("userId", simple("${header.X-User-ID}"))
                .setHeader("X-Service-Request", simple("api/v1/collections/${header.collectionId}"))
                .to("direct:sd-call-finalize");

        from("platform-http:/oapi/v1/books?httpMethodRestrict=GET")
                .routeId("user-books-books-route")
                .onException(ServiceFall.class)
                .handled(true)
                .to("direct:service-error-handler")
                .end()
                .setHeader("X-Headers-Required", constant("bookId"))
                .to("direct:check-params")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader("X-Service", constant("Books"))
                .setHeader("X-Service-Request", simple("api/v1/books/${header.bookId}"))
                .to("direct:sd-call-finalize");

        from("platform-http:/oapi/v1/books/reviews?httpMethodRestrict=GET")
                .routeId("user-books-reviews-books-route")
                .onException(ServiceFall.class)
                .handled(true)
                .to("direct:service-error-handler")
                .end()
                .setHeader("X-Headers-Required", constant("bookId"))
                .to("direct:check-params")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .setHeader("X-Service", constant("Books"))
                .setHeader("X-Service-Request", simple("api/v1/books/${header.bookId}/reviews"))
                .to("direct:sd-call-finalize");
    }
}

