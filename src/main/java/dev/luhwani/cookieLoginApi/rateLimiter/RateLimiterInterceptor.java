package dev.luhwani.cookieLoginApi.rateLimiter;

import java.time.Duration;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class RateLimiterInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;
    private final RateLimiterResponseHelper responseHelper;

    private final Bucket loginEndpointBucket;
        private final Bucket registerEndpointBucket;

    // when the admin enpoint is created, it'll be a different issue

    public RateLimiterInterceptor(RateLimiterService rateLimiterService,
            RateLimiterResponseHelper responseHelper) {
        this.rateLimiterService = rateLimiterService;
        this.responseHelper = responseHelper;

        loginEndpointBucket = Bucket.builder().addLimit(
                    Bandwidth.builder()
                        .capacity(15)
                        .refillGreedy(15, Duration.ofMinutes(1))
                        .build())
                    .build();

        registerEndpointBucket = Bucket.builder().addLimit(
                    Bandwidth.builder()
                        .capacity(10)
                        .refillGreedy(10, Duration.ofMinutes(1))
                        .build())
                    .build();
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        String ip = request.getRemoteAddr();
        String path = request.getRequestURI();

        String endpointKey = resolveEndpointKey(path);

        // If it's not a rate-limited endpoint, let the request through
        if (endpointKey == null) {
            return true;
        }

        ConsumptionProbe probe;

        if (endpointKey.equals("login")) {
            probe = loginEndpointBucket.tryConsumeAndReturnRemaining(1);
        } else {
            probe = registerEndpointBucket.tryConsumeAndReturnRemaining(1);
        }

        if (!probe.isConsumed()) {
            responseHelper.writeRateLimitExceededResponse(response, probe);
            return false;
        }
        
        Bucket bucket = rateLimiterService.resolveBucket(ip, endpointKey);
        probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            responseHelper.addRateLimitHeaders(response, bucket);
            return true;
        } else {
            responseHelper.writeRateLimitExceededResponse(response, probe);
            return false;
        }
    }

    private String resolveEndpointKey(String path) {
        if (path.contains("/login"))
            return "login";
        if (path.contains("/register"))
            return "register";
        return null;
        // when you have admin endpoints, add them here
    }
}