/**
 * @file index.test.ts
 * @description Unit and integration tests for MediaCore V4.0 JWT Token Server
 * @module backend/tests
 */

import { expect, test, describe } from "bun:test";
import app from "./index.ts";

describe("MediaCore V4.0 Token API", () => {
  // Test 1: Valid room and participant names
  test("POST /api/token - Valid request returns 200 OK with token", async () => {
    const res = await app.fetch(
      new Request("http://localhost/api/token", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          roomName: "room_101",
          participantName: "operator_alpha",
        }),
      })
    );

    expect(res.status).toBe(200);
    
    // Check security headers
    expect(res.headers.get("X-Content-Type-Options")).toBe("nosniff");
    expect(res.headers.get("X-Frame-Options")).toBe("DENY");
    expect(res.headers.get("Content-Security-Policy")).toBe("default-src 'none'; frame-ancestors 'none'; sandbox");

    // Check rate limit headers
    expect(res.headers.get("X-RateLimit-Limit")).toBe("100");
    expect(res.headers.get("X-RateLimit-Remaining")).toBeDefined();
    expect(res.headers.get("X-RateLimit-Reset")).toBeDefined();

    const body = await res.json();
    expect(body.token).toBeDefined();
    expect(typeof body.token).toBe("string");
    expect(body.serverUrl).toBe(process.env.LIVEKIT_URL || "wss://mediacore-v4.livekit.cloud");
  });

  // Test 2: Input with forbidden extra properties (Zod strict constraint)
  test("POST /api/token - Reject requests containing extra fields", async () => {
    const res = await app.fetch(
      new Request("http://localhost/api/token", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          roomName: "room_101",
          participantName: "operator_alpha",
          maliciousField: "hack",
        }),
      })
    );

    expect(res.status).toBe(400);
    const body = await res.json();
    expect(body.error).toBe("BAD_REQUEST");
    expect(body.message).toBe("Input validation failed");
  });

  // Test 3: Invalid symbols (strict regex constraint)
  test("POST /api/token - Reject requests containing special characters in identifiers", async () => {
    const res = await app.fetch(
      new Request("http://localhost/api/token", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          roomName: "room;DROP TABLE users;--",
          participantName: "admin",
        }),
      })
    );

    expect(res.status).toBe(400);
    const body = await res.json();
    expect(body.error).toBe("BAD_REQUEST");
  });

  // Test 4: Name values that are too short (min constraints)
  test("POST /api/token - Reject fields below minimum lengths", async () => {
    const res = await app.fetch(
      new Request("http://localhost/api/token", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          roomName: "ro", // Less than 3 characters
          participantName: "a", // Less than 2 characters
        }),
      })
    );

    expect(res.status).toBe(400);
    const body = await res.json();
    expect(body.error).toBe("BAD_REQUEST");
  });
});
