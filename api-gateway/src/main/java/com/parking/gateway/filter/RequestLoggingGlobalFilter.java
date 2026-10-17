package com.parking.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Global pre/post filter.
 *  PRE : assign or propagate X-Correlation-Id, stamp the request, log the inbound call.
 *  POST: log status, route and latency; echo correlation id and latency to the client.
 */
@Slf4j
@Component
public class RequestLoggingGlobalFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    public static final String RECEIVED_AT_HEADER = "X-Gateway-Received-At";
    public static final String LATENCY_HEADER = "X-Response-Time-Ms";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest original = exchange.getRequest();

        String correlationId = Optional.ofNullable(original.getHeaders().getFirst(CORRELATION_HEADER))
                .filter(v -> !v.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());

        ServerHttpRequest request = original.mutate()
                .header(CORRELATION_HEADER, correlationId)
                .header(RECEIVED_AT_HEADER, Instant.now().toString())
                .build();
        ServerWebExchange mutated = exchange.mutate().request(request).build();

        long startNanos = System.nanoTime();
        mutated.getResponse().getHeaders().set(CORRELATION_HEADER, correlationId);
        mutated.getResponse().beforeCommit(() -> {
            long millis = (System.nanoTime() - startNanos) / 1_000_000;
            mutated.getResponse().getHeaders().set(LATENCY_HEADER, String.valueOf(millis));
            return Mono.empty();
        });

        InetSocketAddress remote = request.getRemoteAddress();
        log.info("--> [{}] {} {} from {}", correlationId, request.getMethod(), request.getURI().getRawPath(),
                remote != null && remote.getAddress() != null ? remote.getAddress().getHostAddress() : "unknown");

        return chain.filter(mutated)
                .doOnSuccess(v -> {
                    Route route = mutated.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
                    log.info("<-- [{}] {} {} route={} status={} {}ms", correlationId, request.getMethod(),
                            request.getURI().getRawPath(), route != null ? route.getId() : "n/a",
                            mutated.getResponse().getStatusCode(), (System.nanoTime() - startNanos) / 1_000_000);
                })
                .doOnError(e -> log.error("<-- [{}] {} {} failed after {}ms: {}", correlationId, request.getMethod(),
                        request.getURI().getRawPath(), (System.nanoTime() - startNanos) / 1_000_000, e.toString()));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
