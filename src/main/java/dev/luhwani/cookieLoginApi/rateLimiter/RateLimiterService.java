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

    private Bucket createRegisterBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(7)
                        .refillIntervally(7, Duration.ofMinutes(2))
                        .build())
                .build();
    }

    // if you wish to rate limit "per ip" other endpoints, create more methods like this, 
    // and add the bucket to the resolveBucket method

    public Bucket resolveBucket(String ip, String endpointKey) {
        String key = ip + ":" + endpointKey;
        return switch (endpointKey) {
            case "register" -> buckets.computeIfAbsent(key, k -> createRegisterBucket());
            default         -> throw new IllegalArgumentException("Unknown endpoint key: " + endpointKey);
        };
    }
    
}
