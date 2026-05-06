package com.resumepilot.ai.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class AiUsageLimiterTest {

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-05-06T00:00:00Z"), ZoneId.of("UTC"));

    @Test
    void allowsFreeUserUntilDailyLimitThenBlocks() {
        AiUsageLimiter limiter = new AiUsageLimiter(2, fixedClock);

        AiUsageLimiter.UsageDecision first = limiter.tryConsume("palak@example.com", "FREE", "FREE");
        AiUsageLimiter.UsageDecision second = limiter.tryConsume("palak@example.com", "FREE", "FREE");
        AiUsageLimiter.UsageDecision third = limiter.tryConsume("palak@example.com", "FREE", "FREE");

        assertThat(first.allowed()).isTrue();
        assertThat(first.remaining()).isEqualTo(1);
        assertThat(second.allowed()).isTrue();
        assertThat(second.remaining()).isZero();
        assertThat(third.allowed()).isFalse();
        assertThat(third.limit()).isEqualTo(2);
    }

    @Test
    void paidUsersAreUnlimited() {
        AiUsageLimiter limiter = new AiUsageLimiter(1, fixedClock);

        for (int i = 0; i < 10; i++) {
            AiUsageLimiter.UsageDecision decision = limiter.tryConsume("paid@example.com", "USER", "PRO");
            assertThat(decision.allowed()).isTrue();
            assertThat(decision.remaining()).isEqualTo(Integer.MAX_VALUE);
        }
    }

    @Test
    void normalUsersWithoutPaidPlanUseFreeQuota() {
        AiUsageLimiter limiter = new AiUsageLimiter(1, fixedClock);

        assertThat(limiter.tryConsume("user@example.com", "USER", "USER").allowed()).isTrue();
        assertThat(limiter.tryConsume("user@example.com", "USER", "USER").allowed()).isFalse();
    }

    @Test
    void anonymousUsersShareAnonymousBucket() {
        AiUsageLimiter limiter = new AiUsageLimiter(1, fixedClock);

        assertThat(limiter.tryConsume(null, null, null).allowed()).isTrue();
        assertThat(limiter.tryConsume("   ", "FREE", "FREE").allowed()).isFalse();
    }

    @Test
    void clearAllResetsCounters() {
        AiUsageLimiter limiter = new AiUsageLimiter(1, fixedClock);

        assertThat(limiter.tryConsume("user@example.com", "FREE", "FREE").allowed()).isTrue();
        assertThat(limiter.tryConsume("user@example.com", "FREE", "FREE").allowed()).isFalse();

        limiter.clearAll();

        assertThat(limiter.tryConsume("user@example.com", "FREE", "FREE").allowed()).isTrue();
    }
}
