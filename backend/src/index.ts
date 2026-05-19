/**
 * @file index.ts
 * @description Main entry point for the MediaCore v4.0 JWT Token Server
 * @module backend
 */

import { Hono } from "hono";
import { cors } from "hono/cors";
import { z } from "zod";
import { dualLayerRateLimiter } from "./utils/rateLimiter.ts";
import { generateLiveKitToken } from "./utils/token.ts";

const app = new Hono();

// 1. Structured Logger Middleware
app.use("*", async (c, next) => {
  const start = Date.now();
  const { method, url } = c.req;
  
  await next();
  
  const ms = Date.now() - start;
  // Standard structured JSON logging with redacted payloads
  console.log(JSON.stringify({
    timestamp: new Date().toISOString(),
    method,
    url,
    status: c.res.status,
    durationMs: ms,
    ip: c.req.header("x-forwarded-for") || "127.0.0.1",
  }));
});

// 2. OWASP Security Headers Middleware
app.use("*", async (c, next) => {
  c.header("X-Content-Type-Options", "nosniff");
  c.header("X-Frame-Options", "DENY");
  c.header("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; sandbox");
  c.header("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
  await next();
});

// 3. CORS Whitelist Protection
const isProd = Bun.env.NODE_ENV === "production";
app.use(
  "/api/*",
  cors({
    origin: (origin) => {
      // In production, enforce whitelist. In development, allow localhost.
      if (!isProd) return origin;
      const whitelist = ["https://mediacore-v4.example.com"];
      if (origin && whitelist.includes(origin)) {
        return origin;
      }
      return ""; // Denied origin
    },
    allowMethods: ["POST", "OPTIONS"],
    allowHeaders: ["Content-Type", "Authorization"],
    maxAge: 600,
    credentials: true,
  })
);

// 4. Rate Limiting Middleware
app.use("/api/*", dualLayerRateLimiter());

// 5. Zod Strict Schema for Validation
const TokenRequestSchema = z
  .object({
    roomName: z
      .string()
      .min(3, "Room ID must be at least 3 characters long")
      .max(50, "Room ID cannot exceed 50 characters")
      .regex(/^[a-zA-Z0-9_-]+$/, "Room ID can only contain alphanumeric characters, hyphens, and underscores"),
    participantName: z
      .string()
      .min(2, "Operator Name must be at least 2 characters long")
      .max(50, "Operator Name cannot exceed 50 characters")
      .regex(/^[a-zA-Z0-9_-]+$/, "Operator Name can only contain alphanumeric characters, hyphens, and underscores"),
  })
  .strict(); // Forbid extra fields per Rule 7.B

// 6. Routes
app.post("/api/token", async (c) => {
  try {
    const rawBody = await c.req.json();
    
    // Strict input validation
    const parsed = TokenRequestSchema.safeParse(rawBody);
    if (!parsed.success) {
      c.status(400);
      return c.json({
        error: "BAD_REQUEST",
        message: "Input validation failed",
        details: parsed.error.format(),
      });
    }

    const { roomName, participantName } = parsed.data;

    // Generate signed LiveKit JWT Access Token
    const payload = await generateLiveKitToken({
      roomName,
      participantName,
    });

    return c.json(payload);

  } catch (err: any) {
    console.error(
      JSON.stringify({
        timestamp: new Date().toISOString(),
        error: "INTERNAL_SERVER_ERROR",
        message: err.message,
      })
    );

    c.status(500);
    return c.json({
      error: "INTERNAL_SERVER_ERROR",
      message: "An internal server error occurred while processing token request.",
    });
  }
});

// 7. Start Server
const port = Number(Bun.env.PORT) || 3000;
console.log(`MediaCore V4.0 Backend Server starting on port ${port}...`);

export default {
  port,
  fetch: app.fetch,
};
