package dev.luhwani.cookieLoginApi.rateLimiter;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

@Service
public class RateLimiterService {

    public final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket createLoginBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(5)
                        .refillGreedy(5, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private Bucket createRegisterBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(7)
                        .refillGreedy(7, Duration.ofMinutes(2))
                        .build())
                .build();
    }

    public Bucket resolveBucket(String ip, String endpointKey) {
        String key = ip + ":" + endpointKey;
        return switch (endpointKey) {
            case "login"    -> buckets.computeIfAbsent(key, k -> createLoginBucket());
            case "register" -> buckets.computeIfAbsent(key, k -> createRegisterBucket());
            default         -> throw new IllegalArgumentException("Unknown endpoint key: " + endpointKey);
        };
    }
    
}

/*
EndpointRate Limit?Reason
POST /admin-loginYes — stricter than /loginAdmin accounts are high-value targets
GET /admin-homeLooselyAuthenticated admin route — light limit like /me
GET /meLooselyAuthenticated, low-risk — a light limit is fine
 */