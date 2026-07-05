package com.example.cmslearnenglish.security;

import com.example.cmslearnenglish.dto.RateLimitErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final int MAX_FAILURES = 10;
    private static final long WINDOW_SECONDS = 300L;

    private final ConcurrentHashMap<String, Deque<Long>> failureTimestamps = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !(request.getMethod().equalsIgnoreCase("POST")
                && LOGIN_PATH.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = getClientIp(request);
        long now = System.currentTimeMillis() / 1000L;

        Deque<Long> timestamps = failureTimestamps.computeIfAbsent(ip, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            // Remove timestamps outside the sliding window
            while (!timestamps.isEmpty() && (now - timestamps.peekFirst()) >= WINDOW_SECONDS) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= MAX_FAILURES) {
                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setHeader("Retry-After", String.valueOf(WINDOW_SECONDS));
                objectMapper.writeValue(response.getWriter(),
                        new RateLimitErrorResponse("Quá nhiều lần thử. Vui lòng thử lại sau.", (int) WINDOW_SECONDS));
                return;
            }
        }

        filterChain.doFilter(request, response);

        // Track failure after request completes
        int status = response.getStatus();
        if (status == HttpServletResponse.SC_UNAUTHORIZED || status == HttpServletResponse.SC_BAD_REQUEST) {
            synchronized (timestamps) {
                timestamps.addLast(now);
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
