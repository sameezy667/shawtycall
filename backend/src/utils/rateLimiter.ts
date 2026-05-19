/**
 * @file rateLimiter.ts
 * @description Dual-layer rate limiting middleware for IP and Participant Name (User)
 * @module backend/utils
 */

import type { MiddlewareHandler } from "hono";

interface RateLimitEntry {
  count: number;
  resetTime: number;
}

// In-memory stores
const ipStore = new Map<string, RateLimitEntry>();
const userStore = new Map<string, RateLimitEntry>();

// Clean up expired entries every 5 minutes to prevent memory leaks
setInterval(() => {
  const now = Date.now();
  for (const [key, value] of ipStore.entries()) {
    if (now > value.resetTime) ipStore.delete(key);
  }
  for (const [key, value] of userStore.entries()) {
    if (now > value.resetTime) userStore.delete(key);
  }
}, 5 * 60 * 1000);

/**
 * Dual-layer rate limiter middleware.
 * Checks IP (100 req / 15 min) and optional User (1000 req / 1 hour).
 */
export const dualLayerRateLimiter = (): MiddlewareHandler => {
  return async (c, next) => {
    const now = Date.now();

    // 1. IP Rate Limiting Setup
    const ip = c.req.header("x-forwarded-for") || "127.0.0.1";
    const ipWindowMs = 15 * 60 * 1000; // 15 minutes
    const ipLimit = 100;

    let ipEntry = ipStore.get(ip);
    if (!ipEntry || now > ipEntry.resetTime) {
      ipEntry = { count: 0, resetTime: now + ipWindowMs };
      ipStore.set(ip, ipEntry);
    }

    // 2. User Rate Limiting Setup (if participantName is provided in body)
    // Clone body so we don't consume the stream prematurely
    let participantName: string | undefined;
    try {
      const clonedReq = c.req.raw.clone();
      const body = await clonedReq.json();
      if (body && typeof body === "object" && "participantName" in body) {
        participantName = String(body.participantName);
      }
    } catch {
      // Body not JSON or empty
    }

    const userWindowMs = 60 * 60 * 1000; // 1 hour
    const userLimit = 1000;
    let userEntry: RateLimitEntry | undefined;

    if (participantName) {
      userEntry = userStore.get(participantName);
      if (!userEntry || now > userEntry.resetTime) {
        userEntry = { count: 0, resetTime: now + userWindowMs };
        userStore.set(participantName, userEntry);
      }
    }

    // 3. Increment Counts
    ipEntry.count++;
    if (userEntry) {
      userEntry.count++;
    }

    // Calculate Rate-Limit Headers
    const ipRemaining = Math.max(0, ipLimit - ipEntry.count);
    const ipResetSeconds = Math.max(0, Math.ceil((ipEntry.resetTime - now) / 1000));

    c.header("X-RateLimit-Limit", String(ipLimit));
    c.header("X-RateLimit-Remaining", String(ipRemaining));
    c.header("X-RateLimit-Reset", String(ipResetSeconds));

    // 4. Check Violations
    if (ipEntry.count > ipLimit) {
      c.status(429);
      c.header("Retry-After", String(ipResetSeconds));
      return c.json({
        error: "TOO_MANY_REQUESTS",
        message: "IP rate limit exceeded. Max 100 requests per 15 minutes.",
        details: {
          retryAfterSeconds: ipResetSeconds,
        },
      });
    }

    if (userEntry && userEntry.count > userLimit) {
      const userResetSeconds = Math.max(0, Math.ceil((userEntry.resetTime - now) / 1000));
      c.status(429);
      c.header("Retry-After", String(userResetSeconds));
      return c.json({
        error: "TOO_MANY_REQUESTS",
        message: "User rate limit exceeded. Max 1000 requests per 1 hour.",
        details: {
          retryAfterSeconds: userResetSeconds,
        },
      });
    }

    await next();
  };
};
