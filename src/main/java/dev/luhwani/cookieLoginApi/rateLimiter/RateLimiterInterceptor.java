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

    private static final Bucket registerEndpointBucket = Bucket.builder().addLimit(
                    Bandwidth.builder()
                        .capacity(10)
                        .refillIntervally(10, Duration.ofMinutes(1))
                        .build())
                    .build();

    // any other endpoint that needs to be rate limited will also be placed here
    // except for the login endpoint because Spring will handle login for you

    public RateLimiterInterceptor(RateLimiterService rateLimiterService,
            RateLimiterResponseHelper responseHelper) {
        this.rateLimiterService = rateLimiterService;
        this.responseHelper = responseHelper;
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

        // if other endpoints to be rate limited existed, there would be an if-else, to
        // determine whose bucket should have a consumption probe gotten from

        ConsumptionProbe probe = registerEndpointBucket.tryConsumeAndReturnRemaining(1);;

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