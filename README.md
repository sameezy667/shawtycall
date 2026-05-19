# ShawtyCall - MediaCore V4.0

High-performance native Android video calling app with hardware-accelerated HEVC (H.265) encoding via LiveKit.

## 🚀 Features

- **Native Android App** - Pure Kotlin with Jetpack Compose UI
- **HEVC (H.265) Encoding** - Hardware-accelerated video compression
- **LiveKit Integration** - Ultra-low latency WebRTC via SFU architecture
- **End-to-End Encrypted** - SRTP/DTLS security
- **Material 3 Design** - Modern, industrial-themed UI

## 📁 Project Structure

```
/videoCall
├── frontend/          # Android Native App (Kotlin + Jetpack Compose)
└── backend/           # Token Server (Bun + Hono + TypeScript)
```

## 🛠️ Tech Stack

### Frontend
- Kotlin 1.9+
- Jetpack Compose (Material 3)
- LiveKit Android SDK 2.4.0
- Target SDK 34, Min SDK 26

### Backend
- Bun Runtime
- Hono Framework
- LiveKit Server SDK
- Zod Validation
- Dual-layer Rate Limiting

## 📦 Setup Instructions

### Backend Deployment (Render)

1. **Deploy to Render:**
   - Connect this repo to Render
   - Root Directory: `backend`
   - Build Command: `bun install`
   - Start Command: `bun run src/index.ts`

2. **Environment Variables:**
   ```
   LIVEKIT_API_KEY=your_api_key
   LIVEKIT_API_SECRET=your_api_secret
   LIVEKIT_URL=wss://your-project.livekit.cloud
   PORT=3000
   ```

### Frontend Build (APK)

1. **Update Backend URL:**
   - Open `frontend/app/src/main/java/com/example/hevcapp/MainActivity.kt`
   - Replace `http://localhost:3000` with your Render URL

2. **Build APK:**
   ```bash
   cd frontend
   ./gradlew assembleDebug
   ```

3. **APK Location:**
   ```
   frontend/app/build/outputs/apk/debug/app-debug.apk
   ```

## 🧪 Testing

### Backend Tests
```bash
cd backend
bun test
```

### Android Testing
- Requires physical Android device (SDK 26+)
- Standard emulators lack HEVC hardware encoder support

## 📱 Installation

1. Enable "Install from Unknown Sources" on Android device
2. Transfer APK file to device
3. Tap APK to install
4. Grant Camera and Microphone permissions

## 🔐 Security Features

- JWT-based authentication
- IP + User rate limiting
- OWASP security headers
- Strict input validation (Zod)
- CORS whitelist protection

## 📄 Documentation

- [Product Requirements](./downloadable_prd_markdown_file.md)
- [Design Specifications](./design.md)
- [Technical Context](./context.md)

## 🎯 Architecture

- **SFU Topology** - LiveKit Cloud handles packet forwarding
- **Hardware Acceleration** - Direct MediaCodec API access
- **Fallback Support** - Graceful degradation to VP9/H.264

## 📊 Performance Targets

- **Latency:** <200ms glass-to-glass
- **Resolution:** 720p/1080p HD
- **Bitrate:** 2,500-4,000 kbps
- **Codec:** HEVC (H.265) primary, VP9/H.264 fallback

## 🤝 Contributing

This is a production-ready reference implementation. See documentation files for architecture details.

## 📝 License

MIT License - See LICENSE file for details
