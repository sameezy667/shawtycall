/**
 * @file MainActivity.kt
 * @description Main Activity that manages calling permissions, token fetching, and screen transitions
 * @module frontend
 */

package com.example.hevcapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.example.hevcapp.livekit.LiveKitManager
import com.example.hevcapp.ui.CallScreen
import com.example.hevcapp.ui.LandingScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private lateinit var liveKitManager: LiveKitManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        liveKitManager = LiveKitManager(applicationContext)

        setContent {
            var currentScreen by remember { mutableStateOf("landing") }
            var activeRoomName by remember { mutableStateOf("") }
            var activeParticipantName by remember { mutableStateOf("") }
            var connectionError by remember { mutableStateOf<String?>(null) }

            // Helper scope for network token request
            val composeScope = rememberCoroutineScope()

            // Permission Launcher
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
                val micGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false

                if (cameraGranted && micGranted) {
                    // Fetch token and connect to LiveKit
                    composeScope.launch {
                        try {
                            val tokenPayload = fetchToken(activeRoomName, activeParticipantName)
                            
                            // Connect Room
                            liveKitManager.connect(
                                serverUrl = tokenPayload.first,
                                token = tokenPayload.second,
                                onSuccess = {
                                    currentScreen = "call"
                                },
                                onError = { error ->
                                    connectionError = error
                                }
                            )
                        } catch (e: Exception) {
                            connectionError = "Backend Error: " + (e.message ?: "Failed to fetch token")
                        }
                    }
                } else {
                    connectionError = "Camera and Microphone permissions are required"
                }
            }

            // Screen Share launcher
            val mediaProjectionManager = remember {
                applicationContext.getSystemService(android.content.Context.MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
            }
            val screenShareLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {
                    composeScope.launch {
                        try {
                            liveKitManager.toggleScreenShare(true, result.data!!)
                        } catch (e: Exception) {
                            connectionError = "Screen Share Error: ${e.message}"
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                when (currentScreen) {
                    "landing" -> {
                        LandingScreen(
                            onConnectInitiated = { room, name ->
                                activeRoomName = room
                                activeParticipantName = name
                                connectionError = null

                                // Verify permissions first
                                val hasCamera = ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED

                                val hasMic = ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasCamera && hasMic) {
                                    // Permissions exist, fetch and connect
                                    composeScope.launch {
                                        try {
                                            val tokenPayload = fetchToken(room, name)
                                            liveKitManager.connect(
                                                serverUrl = tokenPayload.first,
                                                token = tokenPayload.second,
                                                onSuccess = {
                                                    currentScreen = "call"
                                                },
                                                onError = { error ->
                                                    connectionError = error
                                                }
                                            )
                                        } catch (e: Exception) {
                                            connectionError = "Backend Error: " + (e.message ?: "Failed to fetch token")
                                        }
                                    }
                                } else {
                                    // Request permissions dynamically
                                    launcher.launch(
                                        arrayOf(
                                            Manifest.permission.CAMERA,
                                            Manifest.permission.RECORD_AUDIO
                                        )
                                    )
                                }
                            },
                            externalError = connectionError
                        )
                    }
                    "call" -> {
                        CallScreen(
                            room = liveKitManager.room,
                            localVideoTrack = liveKitManager.localVideoTrack,
                            remoteVideoTrack = liveKitManager.remoteVideoTrack,
                            remoteScreenShareTrack = liveKitManager.remoteScreenShareTrack,
                            isMuted = liveKitManager.isMuted,
                            isCameraEnabled = liveKitManager.isCameraEnabled,
                            isScreenShareEnabled = liveKitManager.isScreenShareEnabled,
                            localVideoCodec = liveKitManager.localVideoCodec,
                            localVideoResolution = liveKitManager.localVideoResolution,
                            localVideoBitrate = liveKitManager.localVideoBitrate,
                            remoteVideoCodec = liveKitManager.remoteVideoCodec,
                            remoteVideoResolution = liveKitManager.remoteVideoResolution,
                            remoteVideoBitrate = liveKitManager.remoteVideoBitrate,
                            onMuteToggle = { liveKitManager.toggleMute() },
                            onCameraToggle = { liveKitManager.toggleCamera() },
                            onScreenShareToggle = {
                                if (liveKitManager.isScreenShareEnabled) {
                                    composeScope.launch {
                                        liveKitManager.toggleScreenShare(false)
                                    }
                                } else {
                                    try {
                                        screenShareLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                                    } catch (e: Exception) {
                                        connectionError = "Screen Share Permission Launch Failed: ${e.message}"
                                    }
                                }
                            },
                            onEndCall = {
                                liveKitManager.disconnect()
                                currentScreen = "landing"
                            },
                            roomName = activeRoomName
                        )
                    }
                }
            }
        }
    }

    /**
     * Call the Bun Hono backend API to secure a valid Room Token.
     * Maps host to loopback address (10.0.2.2) when running inside emulator.
     */
    private suspend fun fetchToken(roomName: String, participantName: String): Pair<String, String> {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val json = JSONObject().apply {
                put("roomName", roomName)
                put("participantName", participantName)
            }.toString()

            val body = json.toRequestBody(mediaType)
            // Production backend URL on Render
            val request = Request.Builder()
                .url("https://shawtycall.onrender.com/api/token")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code}: ${response.message}")
                }
                val bodyStr = response.body?.string() ?: throw Exception("Empty token payload received")
                val obj = JSONObject(bodyStr)
                
                val rawUrl = obj.getString("serverUrl")
                val token = obj.getString("token")
                
                // Map 127.0.0.1 loopbacks to localhost for adb reverse compatibility
                val finalUrl = if (rawUrl.contains("127.0.0.1")) {
                    rawUrl.replace("127.0.0.1", "localhost")
                } else {
                    rawUrl
                }

                Pair(finalUrl, token)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        liveKitManager.disconnect()
    }
}
