package com.parking.payment.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parking.payment.exception.BadRequestException;
import com.parking.payment.exception.ConflictException;
import com.parking.payment.exception.ResourceNotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;

import java.io.InputStream;

/** 4xx -> domain exceptions (not retried / not recorded by the circuit breaker); 5xx -> FeignException. */
public class DownstreamErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;
    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    public DownstreamErrorDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        if (status >= 400 && status < 500) {
            String message = extractMessage(response);
            return switch (status) {
                case 404 -> new ResourceNotFoundException(message);
                case 409 -> new ConflictException(message);
                default -> new BadRequestException(message);
            };
        }
        return defaultDecoder.decode(methodKey, response);
    }

    private String extractMessage(Response response) {
        if (response.body() == null) {
            return "Downstream request rejected (" + response.status() + ")";
        }
        try (InputStream in = response.body().asInputStream()) {
            JsonNode node = objectMapper.readTree(in);
            return node.path("message").asText("Downstream request rejected (" + response.status() + ")");
        } catch (Exception e) {
            return "Downstream request rejected (" + response.status() + ")";
        }
    }
}
