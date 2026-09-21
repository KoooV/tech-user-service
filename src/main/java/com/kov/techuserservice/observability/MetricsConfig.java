package com.kov.techuserservice.observability;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * P2 Observability: поддержка {@code @Timed} на сервисах/контроллерах.
 * Общие теги ({@code application}) заданы через
 * {@code management.metrics.tags.application} в application.properties.
 * Эндпоинты: {@code /actuator/metrics}, {@code /actuator/prometheus}
 * (exposure: {@code management.endpoints.web.exposure.include}).
 */
@Configuration
public class MetricsConfig {

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
