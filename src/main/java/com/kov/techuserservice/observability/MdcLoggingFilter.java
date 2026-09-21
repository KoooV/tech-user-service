package com.kov.techuserservice.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * P2 Observability: per-request MDC.
 * <ul>
 *   <li>{@code traceId} — из заголовка {@code X-Request-Id} или случайный UUID;
 *       возвращается в ответе тем же заголовком для сквозной трассировки;</li>
 *   <li>{@code userId} — подставляет {@code JwtAuthenticationFilter} (sub из JWT);
 *       здесь только гарантируем отсутствие «хвостов» на переиспользуемых тредах.</li>
 * </ul>
 * Формат вывода — {@code logback-spring.xml}:
 * {@code [traceId=%X{traceId} userId=%X{userId}]}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_KEY = "traceId";
    public static final String USER_ID_KEY = "userId";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(REQUEST_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        MDC.put(TRACE_ID_KEY, traceId);
        // Не тянем userId из прошлого запроса на этом треде из пула.
        MDC.remove(USER_ID_KEY);
        response.setHeader(REQUEST_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
