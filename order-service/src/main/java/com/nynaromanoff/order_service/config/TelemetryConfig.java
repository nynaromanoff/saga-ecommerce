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

    // 🚀 Registra o interceptador nativo de requisições HTTP para gerar o Trace ID
    @Bean
    public Filter serverHttpObservationFilter(ObservationRegistry observationRegistry) {
        return new ServerHttpObservationFilter(observationRegistry);
    }

    // Permite usar a anotação @Observed para monitorar métodos específicos, se necessário
    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }

    // 🚀 O SEGREDO QUE FALTAVA: Força a sincronização do Trace ID com o Slf4j/Logback (MDC)
    @Bean
    public CurrentTraceContext micrometerCurrentTraceContext() {
        return new OtelCurrentTraceContext();
    }
}