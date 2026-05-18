package com.example.demo.common.Mdc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter to populate MDC with request-specific information (like trace ID and authenticated username).
 * This ensures every log statement within the context of an HTTP request can be traced properly in ELK.
 */
@Component
public class MdcWebFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 1. Generate and put traceId
            String traceId = request.getHeader("X-B3-TraceId");
            if (traceId == null || traceId.isEmpty()) {
                traceId = UUID.randomUUID().toString();
            }
            MDC.put("traceId", traceId);

            // 2. Extract username from SecurityContext if available (JWT)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) authentication.getPrincipal();
                String username = jwt.getClaimAsString("preferred_username");
                if (username == null) {
                    username = jwt.getClaimAsString("email");
                }
                if (username != null) {
                    MDC.put("username", username);
                }
            }

            // 3. Add client IP
            String clientIp = request.getHeader("X-Forwarded-For");
            if (clientIp == null || clientIp.isEmpty()) {
                clientIp = request.getRemoteAddr();
            }
            MDC.put("clientIp", clientIp);

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
