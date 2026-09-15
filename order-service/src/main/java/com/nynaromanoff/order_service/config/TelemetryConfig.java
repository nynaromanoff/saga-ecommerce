package com.nynaromanoff.order_service.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ServerHttpObservationFilter;
import jakarta.servlet.Filter;

@Configuration
public class TelemetryConfig {

    @Bean
    public Filter serverHttpObservationFilter(ObservationRegistry observationRegistry) {
        return new ServerHttpObservationFilter(observationRegistry);
    }

    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }

    @Bean
    public CurrentTraceContext micrometerCurrentTraceContext() {
        return new OtelCurrentTraceContext();
    }
}