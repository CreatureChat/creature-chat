// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.chat;

/**
 * A simple token bucket for rate limiting automatic messages.
 */
public class AutoMessageBucket {
    private static final int MAX_TOKENS_CAP = 1000;
    private static final int MAX_COOLDOWN_SECONDS = 3600;

    private final int maxTokens;
    private final int cooldownSeconds;
    private int tokens;
    private long lastRefill;

    public AutoMessageBucket(int maxTokens, int cooldownSeconds) {
        this.maxTokens = Math.max(1, Math.min(maxTokens, MAX_TOKENS_CAP));
        this.cooldownSeconds = Math.max(1, Math.min(cooldownSeconds, MAX_COOLDOWN_SECONDS));
        this.tokens = this.maxTokens;
        this.lastRefill = System.currentTimeMillis();
    }

    /**
     * Refill tokens based on elapsed time.
     */
    public void refill() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRefill;
        int refill = (int) (elapsed / (cooldownSeconds * 1000L));
        if (refill > 0) {
            tokens = Math.min(maxTokens, tokens + refill);
            lastRefill += refill * cooldownSeconds * 1000L;
        }
    }

    /**
     * @return {@code true} if at least one token is available.
     */
    public boolean hasTokens() {
        refill();
        return tokens > 0;
    }

    /**
     * Consume one token. Call only if {@link #hasTokens()} is {@code true}.
     */
    public void consume() {
        if (tokens > 0) {
            tokens--;
        }
    }

    /**
     * Reset to full capacity immediately.
     */
    public void reset() {
        tokens = maxTokens;
        lastRefill = System.currentTimeMillis();
    }
}

