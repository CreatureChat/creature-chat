package com.owlmaddie.utils;


import java.util.concurrent.Semaphore;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The {@code RateLimiter} class is used to slow down LLM unit tests so we don't hit any rate limits accidentally.
 */
public class RateLimiter {
    private final Semaphore semaphore;

    public RateLimiter(int requestsPerSecond) {
        semaphore = new Semaphore(requestsPerSecond);
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            semaphore.release(requestsPerSecond - semaphore.availablePermits());
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void acquire() throws InterruptedException {
        semaphore.acquire();
    }
}