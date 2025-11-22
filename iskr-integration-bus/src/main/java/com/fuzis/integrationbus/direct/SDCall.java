package com.fuzis.integrationbus.direct;

import com.fuzis.integrationbus.exception.AuthenticationException;
import com.fuzis.integrationbus.exception.ServiceDiscoveryFailed;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class SDCall extends RouteBuilder
{
    @Override
    public void configure() throws Exception {
        from("direct:sd-call")
                .onException(ServiceDiscoveryFailed.class)
                    .handled(true)
                    .to("direct:error-sd-fail-handler")
                .end()
                .removeHeader(Exchange.HTTP_PATH)
                .removeHeader(Exchange.HTTP_URI)
                .removeHeader(Exchange.HTTP_URL)
                .choice()
                    .when(header("X-Service").isEqualTo("Accounts"))
                    .toD("${bean:serviceDiscovery?method=getAccountsServiceUrl}/${header.X-Service-Request}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .end();
    }
}
