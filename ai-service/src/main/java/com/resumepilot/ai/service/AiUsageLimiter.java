package com.resumepilot.ai.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AiUsageLimiter {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String PRO_PLAN = "PRO";
    private static final String PREMIUM_PLAN = "PREMIUM";
    private static final String PAID_PLAN = "PAID";
    private static final String ANONYMOUS_USER = "anonymous";

    private final int freeDailyLimit;
    private final Clock clock;
    private final Map<String, UsageCounter> usageCounters = new ConcurrentHashMap<>();
    @Autowired
    public AiUsageLimiter(@Value("${ai.usage.free-daily-limit:5}") int freeDailyLimit) {
        this(freeDailyLimit, Clock.systemDefaultZone());
    }

    AiUsageLimiter(int freeDailyLimit, Clock clock) {
        this.freeDailyLimit = freeDailyLimit;
        this.clock = clock;
    }

    public UsageDecision tryConsume(String userEmail, String userRole, String subscriptionPlan) {
        if (!isFreeUser(userRole, subscriptionPlan)) {
            return UsageDecision.allowed(Integer.MAX_VALUE, freeDailyLimit);
        }

        String normalizedEmail = normalizeEmail(userEmail);
        LocalDate today = LocalDate.now(clock);
        UsageCounter counter = usageCounters.compute(normalizedEmail, (key, existing) -> {
            if (existing == null || !existing.usageDate().equals(today)) {
                return new UsageCounter(today, new AtomicInteger(0));
            }
            return existing;
        });

        int used = counter.count().incrementAndGet();
        int remaining = Math.max(freeDailyLimit - used, 0);
        if (used > freeDailyLimit) {
            counter.count().decrementAndGet();
            return UsageDecision.blocked(0, freeDailyLimit);
        }

        return UsageDecision.allowed(remaining, freeDailyLimit);
    }

    public void clearAll() {
        usageCounters.clear();
    }

    private boolean isFreeUser(String userRole, String subscriptionPlan) {
        String role = normalize(userRole);
        String plan = normalize(subscriptionPlan);
        if (ADMIN_ROLE.equals(role)) {
            return false;
        }
        if (PRO_PLAN.equals(plan) || PREMIUM_PLAN.equals(plan) || PAID_PLAN.equals(plan)) {
            return false;
        }
        return true;
    }

    private String normalizeEmail(String userEmail) {
        String email = userEmail == null ? "" : userEmail.trim().toLowerCase();
        return email.isBlank() ? ANONYMOUS_USER : email;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private record UsageCounter(LocalDate usageDate, AtomicInteger count) {
    }

    public record UsageDecision(boolean allowed, int remaining, int limit) {
        static UsageDecision allowed(int remaining, int limit) {
            return new UsageDecision(true, remaining, limit);
        }

        static UsageDecision blocked(int remaining, int limit) {
            return new UsageDecision(false, remaining, limit);
        }
    }
}
