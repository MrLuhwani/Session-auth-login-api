package dev.luhwani.cookieLoginApi.rateLimiter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimiterResponseHelper {

    private final ObjectMapper objectMapper;
    public RateLimiterResponseHelper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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
