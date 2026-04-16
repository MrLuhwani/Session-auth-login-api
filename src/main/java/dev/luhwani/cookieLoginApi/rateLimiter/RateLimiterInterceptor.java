package dev.luhwani.cookieLoginApi.rateLimiter;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class RateLimiterInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;
    private final RateLimiterResponseHelper responseHelper;

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

        Bucket bucket = rateLimiterService.resolveBucket(ip, endpointKey);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            responseHelper.addRateLimitHeaders(response, bucket);
            return true;
        } else {
            responseHelper.writeRateLimitExceededResponse(response, probe);
            return false;
        }
    }

    private String resolveEndpointKey(String path) {
        if (path.contains("/admin/register"))
            return "admin-register";
        if (path.contains("/register"))
            return "register";
        return null;
    }
}