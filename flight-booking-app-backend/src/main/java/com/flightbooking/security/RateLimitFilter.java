package com.flightbooking.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// Rate limits login and registration attempts per IP address to slow down
// brute-force credential stuffing and account enumeration attacks.
// Bucket4j uses a token-bucket algorithm: each IP gets a bucket of 5 tokens
// that refill fully every minute. Each request consumes one token.
// Once empty, further requests get a 429 until the bucket refills.
//
// This is in-memory and per-instance — fine for a single-server deployment
// like Render's free tier. A multi-instance production deployment would
// back this with Redis instead of a local ConcurrentHashMap.

@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int CAPACITY = 5;
    private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        boolean isRateLimited = path.equals("/api/auth/login") || path.equals("/api/auth/register");

        if (!isRateLimited) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = extractClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, k -> newBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Too many attempts. Please wait a minute before trying again.\"}"
            );
        }
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(CAPACITY, Refill.intervally(CAPACITY, REFILL_PERIOD));
        return Bucket.builder().addLimit(limit).build();
    }

    private String extractClientIp(HttpServletRequest request) {
        // X-Forwarded-For is set by reverse proxies (Render, nginx, etc.)
        // Take the first IP in the chain — that's the original client.
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
