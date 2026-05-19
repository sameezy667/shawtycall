# Product Requirement Document (PRD)
## Project Name: Android Native HEVC Video Call Application
**Document Version:** 2.0 (Updated Architecture & Network Topology Rationale)

---

## 1. Project Objective
Develop a high-performance, native Android video calling application utilizing the LiveKit ecosystem. The primary technical requirement is to enforce hardware-accelerated HEVC (H.265) video encoding to achieve maximum compression density and visual fidelity over mobile networks.

---

## 2. Architectural Rationale & Paradigm Shifts

### 2.1. Why Native Kotlin over Web/Capacitor?
The project strictly avoids cross-platform WebViews (Capacitor/React Native Web) due to OS-level limitations.

- **The WebView Bottleneck:** The Android System WebView frequently disables WebRTC H.265 encoding due to licensing constraints, even if the physical device possesses an HEVC hardware encoder.
- **The Native Solution:** A pure native Kotlin app using the `io.livekit:livekit-android` SDK bypasses the browser engine entirely, granting direct, unhindered access to the Android `MediaCodec` API to enforce hardware-accelerated H.265.

### 2.2. Why WebRTC for Transport?
Although WebRTC's support for H.265 is historically fragmented, it remains the mandatory framework for this application due to its transport architecture, not its codec library:

- **Ultra-Low Latency:** Utilizes UDP via RTP/SRTP to achieve sub-200ms glass-to-glass latency, dropping late packets rather than buffering (unlike TCP-based HLS/DASH).
- **NAT Traversal:** Built-in ICE, STUN, and TURN protocols navigate complex mobile carrier firewalls.
- **Mandatory Security:** Enforces DTLS for connection handshakes and SRTP for end-to-end media payload encryption.

### 2.3. Why SFU (Selective Forwarding Unit) over P2P?
A direct Peer-to-Peer (P2P) topology is explicitly rejected for this mobile-first architecture. All traffic must route through LiveKit Cloud (SFU).

- **Cellular Network Barriers:** Mobile 5G/4G networks employ Carrier-Grade NAT (Symmetric NAT), which actively blocks inbound direct P2P socket connections. The SFU acts as a globally accessible public node.
- **Upstream Bandwidth Preservation:** In a 3+ person call, P2P requires a device to encode and upload its 4,000 kbps H.265 stream multiple times (Full Mesh). An SFU allows the device to upload once, while the server handles packet replication and forwarding.
- **Jitter & Packet Loss:** The SFU maintains an edge cache to instantly re-transmit dropped packets caused by fluctuating cellular radio conditions, preventing severe video macro-blocking.

---

## 3. Technical Stack Architecture

- **Platform:** Android (Minimum SDK 26, Target SDK 34+).
- **Language:** Kotlin.
- **UI Framework:** Jetpack Compose (Declarative UI).
- **Network/Media Infrastructure:** LiveKit Android SDK (`io.livekit:livekit-android`).
- **Build System:** Gradle (Kotlin DSL `build.gradle.kts`).
- **Signaling, SFU & Routing:** LiveKit Cloud.
- **Authentication:** Lightweight backend script (Node/Go/Kotlin) to generate JWT Access Tokens.

---

## 4. Core Technical Specifications

### 4.1. Video Quality & Codec Architecture

- **Primary Codec:** HEVC (H.265). Enforced explicitly via the LiveKit SDK during track publication:

```kotlin
VideoPublishOptions(videoCodec = VideoCodec.H265)
```

- **Hardware Execution:** Relies exclusively on the physical Android device's `MediaCodec` API.
- **Target Resolution:** 1280x720 (720p HD) or 1920x1080 (1080p Full HD).
- **Target Bitrate:** 2,500 kbps to 4,000 kbps dynamic allocation.
- **Mandatory Fallback:** The application must gracefully negotiate a fallback to VP9 or H.264 if the sender lacks an HEVC hardware encoder, or if the receiver lacks an HEVC decoder.

### 4.2. Security & Transport

- **Encryption:** 100% End-to-End Encrypted via SRTP.
- **Handshake:** DTLS enforcement.
- **Authentication:** JWT (JSON Web Token) containing `roomJoin`, `canPublish`, and `canSubscribe` grants.

---

## 5. User Interface Specifications

### 5.1. Screen 1: Authentication & Landing View

- **Aesthetic:** Dark mode default. Minimalist layout utilizing Material 3 Compose components.
- **Inputs:** Text fields for "Room Name" and "Participant Name".
- **Action:** Primary CTA button to request system permissions and initiate LiveKit connection.
- **Permissions Flow:** App must trigger native Android permission dialogs for:

```text
android.permission.CAMERA
android.permission.RECORD_AUDIO
```

### 5.2. Screen 2: Active Call Grid View

- **Remote Participant:** Full-screen render or dynamic grid split using LiveKit's `VideoRenderer` component.
- **Local Participant:** Picture-in-picture (PiP) floating overlay or secondary split. Mirrored horizontally for natural self-view.
- **Control Dock:** Fixed overlay containing:
  - Toggle Microphone (Mute/Unmute).
  - Toggle Camera (Enable/Disable).
  - End Call (Disconnects `Room` and pops back to Landing View).

---

## 6. File Structure Blueprint

```text
android-hevc-call/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/hevcapp/
│   │   │   │   ├── MainActivity.kt        # Compose navigation host
│   │   │   │   ├── ui/
│   │   │   │   │   ├── CallScreen.kt      # Active call UI and LiveKit VideoRenderer
│   │   │   │   │   └── LandingScreen.kt   # Auth inputs and token fetching
│   │   │   │   └── livekit/
│   │   │   │       └── LiveKitManager.kt  # SDK connection, room state, H.265 logic
│   │   │   └── AndroidManifest.xml        # Network & Media permissions
│   ├── build.gradle.kts                   # App-level dependencies (LiveKit SDK)
├── build.gradle.kts                       # Project-level configuration
└── settings.gradle.kts                    # Gradle module inclusions
```

---

## 7. Testing & Validation Metrics

### 7.1. Mandatory Hardware Testing Constraint

- **Constraint:** Testing must be conducted on physical Android devices (e.g., Snapdragon 8 Gen 2 or newer recommended for optimal HEVC testing).
- **Reasoning:** Standard Android Studio emulators do not support hardware H.265 encoding. Attempting to test this specific architecture on an emulator will result in false-negative codec negotiation failures.

### 7.2. Success Criteria

- **Codec Verification:** Utilizing WebRTC inspection tools or LiveKit Cloud Dashboard, the outbound video payload type must map to H.265.
- **Hardware Engagement:** Device CPU utilization must remain within acceptable thermal limits, confirming `MediaCodec` hardware acceleration is actively encoding rather than defaulting to a software pipeline.
- **Latency:** Glass-to-glass latency must remain under 200ms over standard 5G/Wi‑Fi environments.

