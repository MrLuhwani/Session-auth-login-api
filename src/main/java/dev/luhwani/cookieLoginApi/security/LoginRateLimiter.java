package dev.luhwani.cookieLoginApi.security;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoginRateLimiter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    private static final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimiter.class);

    public LoginRateLimiter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private static final Bucket loginEndpointBucket = Bucket.builder().addLimit(
            Bandwidth.builder()
                    .capacity(15)
                    .refillIntervally(15, Duration.ofMinutes(1))
                    .build())
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();

        if (!path.contains("/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = request.getParameter("email");
        String ip = request.getRemoteAddr();
        
        if (email.isEmpty()) {
            log.warn("Null email sent from ip: {}", ip);
            throw new AuthenticationCredentialsNotFoundException("Email not found in request body");
        }

        ConsumptionProbe probe = loginEndpointBucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            writeRateLimitExceededResponse(response, probe);
            return;
        }

        Bucket bucket = buckets.computeIfAbsent(ip, i -> createLoginBucket());

        probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            addRateLimitHeaders(response, bucket);
            filterChain.doFilter(request, response);
        } else {
            writeRateLimitExceededResponse(response, probe);
            return;
        }
    }

    private Bucket createLoginBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(5)
                        .refillGreedy(5, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    public void addRateLimitHeaders(HttpServletResponse response, Bucket bucket) {
        long availableTokens = bucket.getAvailableTokens();
        response.setHeader("X-RateLimit-Remaining", String.valueOf(availableTokens));
    }

    public void writeRateLimitExceededResponse(
            HttpServletResponse response,
            ConsumptionProbe probe) throws IOException {

        long retryAfterSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
        
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setHeader("X-RateLimit-Remaining", "0");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 429);
        body.put("error", "Too Many Requests");
        body.put("message", "Rate limit exceeded. Try again in " + retryAfterSeconds + " seconds.");
        body.put("retryAfterSeconds", retryAfterSeconds);

        objectMapper.writeValue(response.getOutputStream(), body);

    }

}

