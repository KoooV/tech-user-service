package com.kov.techuserservice.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.kov.techuserservice.service.JwtService;
import com.kov.techuserservice.security.CustomUserDetailsServiceImpl;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            putCurrentUserIdToMdc();
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            MDC.remove(com.kov.techuserservice.observability.MdcLoggingFilter.USER_ID_KEY);
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtService.tokenIsValid(token)) {
            MDC.remove(com.kov.techuserservice.observability.MdcLoggingFilter.USER_ID_KEY);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.extractAllClaims(token);
            Long userId = claims.getSubject() != null ? Long.valueOf(claims.getSubject()) : null;

            UserDetails userDetails = userDetailsService.loadUserById(userId);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            // P2 Observability: трассировка — userId в MDC для всех последующих логов запроса.
            MDC.put(com.kov.techuserservice.observability.MdcLoggingFilter.USER_ID_KEY, String.valueOf(userId));
        } catch (Exception e) {
            MDC.remove(com.kov.techuserservice.observability.MdcLoggingFilter.USER_ID_KEY);
            log.debug("JWT authentication failed: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private void putCurrentUserIdToMdc() {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication.getPrincipal() == null) {
                return;
            }
            Object principal = authentication.getPrincipal();
            if (principal instanceof com.kov.techuserservice.entity.User user && user.getId() != null) {
                MDC.put(com.kov.techuserservice.observability.MdcLoggingFilter.USER_ID_KEY,
                        String.valueOf(user.getId()));
            }
        } catch (Exception e) {
            log.debug("MDC userId propagation failed: {}", e.getMessage());
        }
    }
}