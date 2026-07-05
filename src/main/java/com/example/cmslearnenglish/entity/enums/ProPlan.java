package com.example.cmslearnenglish.entity.enums;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public enum ProPlan {
    MONTHLY("PRO 1 month", 69_000L, 30L),
    QUARTERLY("PRO 3 months", 169_000L, 90L),
    YEARLY("PRO 1 year", 499_000L, 365L),
    LIFETIME("PRO lifetime", 1_849_000L, null);

    private static final Instant LIFETIME_EXPIRY = Instant.parse("9999-12-31T23:59:59Z");

    private final String displayName;
    private final long amount;
    private final Long durationDays;

    ProPlan(String displayName, long amount, Long durationDays) {
        this.displayName = displayName;
        this.amount = amount;
        this.durationDays = durationDays;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getAmount() {
        return amount;
    }

    public int getRank() {
        return switch (this) {
            case MONTHLY -> 1;
            case QUARTERLY -> 2;
            case YEARLY -> 3;
            case LIFETIME -> 4;
        };
    }

    public static boolean isLifetimeExpiry(Instant expiry) {
        return expiry != null && !expiry.isBefore(LIFETIME_EXPIRY);
    }

    public Instant calculateExpiry(Instant currentExpiry, Instant now) {
        if (this == LIFETIME || isLifetimeExpiry(currentExpiry)) {
            return LIFETIME_EXPIRY;
        }
        Instant start = currentExpiry != null && currentExpiry.isAfter(now) ? currentExpiry : now;
        return start.plus(durationDays, ChronoUnit.DAYS);
    }
}
