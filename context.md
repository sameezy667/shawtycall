# context.md

## Project Overview
High-performance native Android video calling app. Uses hardware-accelerated HEVC (H.265) encoding via LiveKit SDK. Bypasses WebView limitations by using Kotlin native direct MediaCodec access. Connects via LiveKit Cloud SFU for ultra-low latency cellular transport.

---

## Tech Stack
- **Frontend (Android Native):**
  - Kotlin 1.9+
  - Jetpack Compose (Declarative UI, Material 3)
  - LiveKit Android SDK (`io.livekit:livekit-android`)
  - Gradle (Kotlin DSL, Target SDK 34, Min SDK 26)
- **Backend (Token Signer):**
  - Bun Runtime (v1.1+)
  - TypeScript
  - Hono Framework
  - `@hono/rate-limit` (Dual-layer rate limiting)
  - LiveKit Server SDK (`livekit-server-sdk`)
  - Zod (Strict validation)

---

## Architecture
```
/videoCall
├── frontend/             # Jetpack Compose Native Android App
│   ├── app/
│   │   ├── src/main/java/com/example/hevcapp/
│   │   │   ├── MainActivity.kt
│   │   │   ├── ui/
│   │   │   │   ├── LandingScreen.kt
│   │   │   │   └── CallScreen.kt
│   │   │   └── livekit/
│   │   │       └── LiveKitManager.kt
│   │   └── build.gradle.kts
│   ├── build.gradle.kts
│   └── settings.gradle.kts
└── backend/              # Bun Hono Token Server
    ├── src/
    │   ├── index.ts
    │   └── utils/
    ├── package.json
    ├── tsconfig.json
    └── .env.example
```

### Data Flow
1. **Auth & Setup:** Android App displays Landing Screen. User enters `Room Name` and `Participant Name`.
2. **Token Fetch:** Android App requests JWT from `/api/token` (POST).
3. **Validation & Sign:** Backend Hono app rate-limits IP/User, validates input via Zod, generates LiveKit token with Room grants.
4. **Connect SFU:** Client connects to LiveKit Cloud SFU with token.
5. **Media Publish:** Client initiates H.265 webcam track. SFU forwards to other peers.

---

## Feature Status Checklist
- [x] **Phase 1: Backend Token Server**
  - [x] Initialize Bun-Hono app in `/backend`
  - [x] Implement dual-layer rate limiting (IP + User key)
  - [x] Add Zod strict input validation for room/participant names
  - [x] Implement JWT token generation via LiveKit Server SDK
- [x] **Phase 2: Android Native Client Core**
  - [x] Setup Android Jetpack Compose project under `/frontend`
  - [x] Add LiveKit Android SDK dependency & permissions in `AndroidManifest.xml`
  - [x] Implement runtime camera and mic permission requests
  - [x] Create `LiveKitManager.kt` for core connection and event handling
- [x] **Phase 3: Android UI & Call Flow**
  - [x] Build Material 3 `LandingScreen.kt` for credentials
  - [x] Build dynamic grid `CallScreen.kt` with local/remote `VideoRenderer`
  - [x] Implement overlay Control Dock (Mute, Camera toggle, End call)
- [x] **Phase 4: HEVC Encoding Enforcement & Validation**
  - [x] Set `VideoTrackPublishOptions(videoCodec = "h265")` in Android publisher to enforce H.265 hardware encoding.
  - [x] Implement fallback negotiation to VP9/H.264 if HEVC is unavailable.
  - [x] Integrate Kotlin Flow `room.events` collect architecture (LiveKit v2.x spec) resolving deprecated Listener APIs.
  - [x] Clean compilation of native Android App (`BUILD SUCCESSFUL` via embedded JDK).
  - [x] Enable HTTP cleartext traffic in `AndroidManifest.xml` to allow localhost bridge connectivity on physical devices.
  - [x] Fully passing backend Honoserver tests (`4/4 passing tests`).

---

## Data Models (TS/SQL)
### Token Request
```typescript
interface TokenRequest {
  roomName: string;        // Max 50 chars, regex: ^[a-zA-Z0-9_-]+$
  participantName: string; // Max 50 chars, regex: ^[a-zA-Z0-9_-]+$
}
```

### Token Response
```typescript
interface TokenResponse {
  token: string;
  serverUrl: string;
}
```

---

## API Contracts
### `POST /api/token`
- **Description:** Generates a short-lived room connection token.
- **Request Headers:**
  - `Content-Type: application/json`
- **Request Payload:**
  ```json
  {
    "roomName": "lobby",
    "participantName": "user_1"
  }
  ```
- **Response Headers:**
  - `X-RateLimit-Limit: 100`
  - `X-RateLimit-Remaining: 99`
  - `X-RateLimit-Reset: 900`
- **Response Payload (200 OK):**
  ```json
  {
    "token": "eyJhbGciOi...",
    "serverUrl": "wss://your-livekit-project.livekit.cloud"
  }
  ```
- **Response Payload (400 Bad Request):**
  ```json
  {
    "error": "BAD_REQUEST",
    "message": "Validation failed",
    "details": {
      "roomName": "Required field"
    }
  }
  ```
- **Response Payload (429 Too Many Requests):**
  ```json
  {
    "error": "TOO_MANY_REQUESTS",
    "message": "Rate limit exceeded. Try again in 900 seconds."
  }
  ```

---

## Technical Debt / Next Steps
- **Physical Verification:** Standard emulators lack HEVC hardware encoders; physical testing on a real Android device is required to verify device-level hardware H.265 publication.
- **Production URL Configuration:** The current codebase is ready for production deploy; endpoint addresses should be updated in `.env` and `MainActivity.kt` for live environment routing.

---

## Local Development Environment Verification
- **Backend Development Server:** Running and active at `http://localhost:3000` via Bun.
- **Android SDK Path:** Corrected in `local.properties` to `C:\Users\samee\AppData\Local\Android\Sdk`.
- **ADB Command for current PC:**
  ```powershell
  & "C:\Users\samee\AppData\Local\Android\Sdk\platform-tools\adb.exe" reverse tcp:3000 tcp:3000
  ```
  *(Note: Requires a connected physical device or emulator to execute successfully)*
- **Frontend Build Status:** Android native app successfully compiled via `./gradlew assembleDebug` (`BUILD SUCCESSFUL` in 3m 33s) and successfully installed and launched on physical device `CPH2585` (Android 14+).

