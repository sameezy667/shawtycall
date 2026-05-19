/**
 * @file CallScreen.kt
 * @description Active call view with remote video renderer, draggable PIP local preview, and status overlay indicators
 * @module frontend/ui
 */

package com.example.hevcapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.RemoteVideoTrack
import io.livekit.android.room.track.VideoTrack
import kotlin.math.roundToInt

@Composable
fun CallScreen(
    room: Room?,
    localVideoTrack: LocalVideoTrack?,
    remoteVideoTrack: RemoteVideoTrack?,
    isMuted: Boolean,
    isCameraEnabled: Boolean,
    isScreenShareEnabled: Boolean,
    onMuteToggle: () -> Unit,
    onCameraToggle: () -> Unit,
    onScreenShareToggle: () -> Unit,
    onEndCall: () -> Unit,
    roomName: String,
    localVideoCodec: String = "Unknown",
    localVideoResolution: String = "0x0",
    localVideoBitrate: String = "0 kbps",
    remoteVideoCodec: String = "Unknown",
    remoteVideoResolution: String = "0x0",
    remoteVideoBitrate: String = "0 kbps",
    remoteScreenShareTrack: RemoteVideoTrack? = null,
    modifier: Modifier = Modifier
) {
    // DRAGGABLE PIP self-view position state
    var pipOffset by remember { mutableStateOf(Offset(0f, 0f)) }

    // Generate military looking hex session ID from roomName
    val sessionHex = remember(roomName) {
        val hash = roomName.hashCode().coerceAtLeast(0)
        "0X" + String.format("%04X", hash).take(4)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        
        // 1. MAIN REMOTE VIDEO STREAM (Fills Viewport)
        Box(modifier = Modifier.fillMaxSize()) {
            // Prioritize screen share if available, otherwise show camera
            val displayTrack = remoteScreenShareTrack ?: remoteVideoTrack
            
            if (displayTrack != null && room != null) {
                VideoRenderer(
                    room = room,
                    track = displayTrack,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Standby / Empty room view
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F0F0F)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "WAITING FOR PARTICIPANT",
                            style = TextStyle(
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }
        }

        // 2. OVERLAY STATUS INDICATORS & LABELS
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Secure Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = "Secure Session",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "SESSION_ID: $sessionHex",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0x22FFFFFF), shape = RoundedCornerShape(18.dp))
                        .border(1.dp, Color(0x44FFFFFF), shape = RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Active Call User",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Remote Participant Info (Top-Left overlay)
            if (remoteVideoTrack != null || remoteScreenShareTrack != null) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xAA000000))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .border(1.dp, Color(0x33FFFFFF), shape = RoundedCornerShape(0.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.Red)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (remoteScreenShareTrack != null) "REMOTE SCREEN SHARE" else "REMOTE PARTICIPANT",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0x77000000), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "CODEC: $remoteVideoCodec  |  RES: $remoteVideoResolution  |  BITRATE: $remoteVideoBitrate",
                            style = TextStyle(
                                color = Color(0xFFCCCCCC),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Labels (Above controls dock)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Signal Indicator Bars (Bottom Left)
                Column {
                    Text(
                        text = "NETWORK STABILITY INDEX",
                        style = TextStyle(
                            color = Color.Gray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        SignalBar(height = 12.dp, active = true)
                        Spacer(modifier = Modifier.width(3.dp))
                        SignalBar(height = 18.dp, active = true)
                        Spacer(modifier = Modifier.width(3.dp))
                        SignalBar(height = 24.dp, active = true)
                        Spacer(modifier = Modifier.width(3.dp))
                        SignalBar(height = 30.dp, active = true)
                    }
                }

                // Secure Link Tunnel (Bottom Right)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "SECURE LINK ESTABLISHED",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "TUNNEL: 09-XF-44-22",
                        style = TextStyle(
                            color = Color.Gray,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }

            // 3. FIXED CONTROLS DOCK (At the absolute bottom)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .background(Color.Black)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Mute Toggle Button
                DockIconButton(
                    icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                    label = "MUTE",
                    active = false,
                    onClick = onMuteToggle
                )

                // Video/Camera Toggle Button (Highlighted state per screenshot: White background card)
                DockIconButton(
                    icon = if (isCameraEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                    label = "VIDEO",
                    active = true,
                    onClick = onCameraToggle
                )

                // Share Toggle Button
                DockIconButton(
                    icon = Icons.Filled.ScreenShare,
                    label = "SHARE",
                    active = isScreenShareEnabled,
                    onClick = onScreenShareToggle
                )

                // Grid View Toggle Button (Placeholder)
                DockIconButton(
                    icon = Icons.Filled.GridView,
                    label = "GRID",
                    active = false,
                    onClick = {}
                )

                // End Call Red Button
                Box(
                    modifier = Modifier
                        .size(width = 72.dp, height = 52.dp)
                        .background(Color(0xFFB71C1C), shape = RoundedCornerShape(0.dp))
                        .clickable { onEndCall() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 4. PICTURE-IN-PICTURE DRAGGABLE CARD OVERLAY (Top-Right Default)
        Box(
            modifier = Modifier
                .offset { IntOffset(pipOffset.x.roundToInt(), pipOffset.y.roundToInt()) }
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 20.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        pipOffset += dragAmount
                    }
                }
                .size(width = 130.dp, height = 170.dp)
                .background(Color.Black)
                .border(1.dp, Color(0x88FFFFFF), shape = RoundedCornerShape(0.dp))
        ) {
            // Local Camera stream inside PiP
            if (isCameraEnabled && localVideoTrack != null && room != null) {
                VideoRenderer(
                    room = room,
                    track = localVideoTrack,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF151515)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.VideocamOff,
                        contentDescription = "Camera Muted",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Top-right encryption / specs stamp overlay
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "CODEC: $localVideoCodec",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Text(
                    text = "RES: $localVideoResolution",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 7.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Text(
                    text = "BITRATE: $localVideoBitrate",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 7.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }

            // Bottom-left "[YOU]" self identifier overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .border(1.dp, Color(0x66FFFFFF), shape = RoundedCornerShape(0.dp))
                    .background(Color(0x88000000))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "YOU",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }
    }
}

@Composable
fun SignalBar(height: androidx.compose.ui.unit.Dp, active: Boolean) {
    Box(
        modifier = Modifier
            .width(4.dp)
            .height(height)
            .background(if (active) Color.White else Color.DarkGray)
    )
}

@Composable
fun DockIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    if (active) {
        // Highlighted state: White background card, black elements
        Box(
            modifier = Modifier
                .size(width = 68.dp, height = 52.dp)
                .background(Color.White, shape = RoundedCornerShape(0.dp))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    style = TextStyle(
                        color = Color.Black,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }
    } else {
        // Standard state: transparent card, white elements
        Box(
            modifier = Modifier
                .size(width = 68.dp, height = 52.dp)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }
    }
}

@Composable
fun VideoRenderer(
    room: Room,
    track: VideoTrack,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            TextureViewRenderer(context).apply {
                room.initVideoRenderer(this)
                track.addRenderer(this)
            }
        },
        onRelease = { view ->
            track.removeRenderer(view)
            view.release()
        },
        modifier = modifier
    )
}

