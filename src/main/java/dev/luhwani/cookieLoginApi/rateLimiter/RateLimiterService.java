package dev.luhwani.cookieLoginApi.rateLimiter;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

@Service
public class RateLimiterService {

    public static final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket createRegisterBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(7)
                        .refillIntervally(7, Duration.ofMinutes(2))
                        .build())
                .build();
    }

    private Bucket createAdminRegisterBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(3)
                        .refillIntervally(1, Duration.ofMinutes(2))
                        .build())
                .build();
    }

    public Bucket resolveBucket(String ip, String endpointKey) {
        String key = ip + ":" + endpointKey;
        return switch (endpointKey) {
            case "register" -> buckets.computeIfAbsent(key, k -> createRegisterBucket());
            case "admin-register" -> buckets.computeIfAbsent(key, k -> createAdminRegisterBucket());
            default -> throw new IllegalArgumentException("Unknown endpoint key: " + endpointKey);
        };
    }
    
}
