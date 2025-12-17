package com.fuzis.integrationbus.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuzis.integrationbus.configuration.SSOConfiguration;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ProcessorUtils {
    public static enum SSORequestBodyType{
        JSON,
        URLENCODED
    }
    private final SSOConfiguration ssoConfiguration;

    private final ObjectMapper objectMapper;

    private final FormatEncoder formatEncoder;

    @Autowired
    public ProcessorUtils(SSOConfiguration ssoConfiguration, ObjectMapper objectMapper, FormatEncoder formatEncoder) {
        this.ssoConfiguration = ssoConfiguration;
        this.objectMapper = objectMapper;
        this.formatEncoder = formatEncoder;
    }

    public <T> Integer ssoRequest(ProducerTemplate producerTemplate, Exchange exchange,
                             String httpEndpoint, Map<String, Object> request, Map<String,String> headers, SSORequestBodyType type) throws Exception {

        Exchange responseExchange = producerTemplate.request(httpEndpoint, ex -> {

            if(type == SSORequestBodyType.URLENCODED){
                ex.getIn().setHeader(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded");
                ex.getIn().setBody(formatEncoder.encodeMapUrlEncoded(request));
            }
            else if (type == SSORequestBodyType.JSON){
                ex.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
                ex.getIn().setBody(objectMapper.writeValueAsString(request));
            }
            if(headers != null) for(var el : headers.entrySet()){
                ex.getIn().setHeader(el.getKey(), el.getValue());
            }
        });

        Integer return_code = responseExchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        String response_json = responseExchange.getMessage().getBody(String.class);
        try {
            T response = response_json == null ? null : objectMapper.readValue(response_json, new TypeReference<>() {});
            exchange.getIn().setBody(response);
        } catch (Exception e) {
            exchange.getIn().setBody("");
        }
        return return_code;
    }
}
