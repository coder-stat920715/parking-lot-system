package com.parking.payment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parking.payment.client.DownstreamErrorDecoder;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public ErrorDecoder errorDecoder(ObjectMapper objectMapper) {
        return new DownstreamErrorDecoder(objectMapper);
    }

    @Bean
    public RequestInterceptor correlationIdInterceptor() {
        return template -> {
            String id = MDC.get(CorrelationIdFilter.MDC_KEY);
            if (id != null) {
                template.header(CorrelationIdFilter.HEADER, id);
            }
        };
    }
}
