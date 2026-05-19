/**
 * @file LiveKitManager.kt
 * @description Manages LiveKit SDK room connections, H.265 publishing, and device track toggles
 * @module frontend/livekit
 */

package com.example.hevcapp.livekit

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import io.livekit.android.events.RoomEvent
import io.livekit.android.room.participant.LocalParticipant
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.participant.RemoteParticipant
import io.livekit.android.room.participant.VideoTrackPublishOptions
import io.livekit.android.room.track.LocalAudioTrack
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.RemoteVideoTrack
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoTrack
import io.livekit.android.room.track.VideoCodec
import io.livekit.android.events.collect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class LiveKitManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main)
    var room: Room? = null
        private set

    // State bindings for Jetpack Compose UI
    var connectionState by mutableStateOf(Room.State.DISCONNECTED)
        private set

    var localVideoTrack by mutableStateOf<LocalVideoTrack?>(null)
        private set

    var remoteVideoTrack by mutableStateOf<RemoteVideoTrack?>(null)
        private set

    var isMuted by mutableStateOf(false)
        private set

    var isCameraEnabled by mutableStateOf(true)
        private set

    var isScreenShareEnabled by mutableStateOf(false)
        private set

    var activeRemoteParticipant by mutableStateOf<Participant?>(null)
        private set

    private var localAudioTrack: LocalAudioTrack? = null

    /**
     * Connect to the LiveKit Room and publish media.
     */
    fun connect(serverUrl: String, token: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        scope.launch {
            try {
                // Initialize room instance
                val activeRoom = LiveKit.create(context)
                room = activeRoom

                // Setup Event Flow Collection
                scope.launch {
                    activeRoom.events.collect { event ->
                        when (event) {
                            is RoomEvent.Connected -> {
                                connectionState = Room.State.CONNECTED
                                onSuccess()
                            }
                            is RoomEvent.Disconnected -> {
                                connectionState = Room.State.DISCONNECTED
                                cleanUp()
                            }
                            is RoomEvent.TrackSubscribed -> {
                                val track = event.track
                                val participant = event.participant
                                if (track is RemoteVideoTrack) {
                                    remoteVideoTrack = track
                                    activeRemoteParticipant = participant
                                }
                            }
                            is RoomEvent.TrackUnsubscribed -> {
                                val track = event.track
                                if (track is RemoteVideoTrack) {
                                    remoteVideoTrack = null
                                    activeRemoteParticipant = null
                                }
                            }
                            is RoomEvent.ParticipantDisconnected -> {
                                val participant = event.participant
                                if (activeRemoteParticipant == participant) {
                                    remoteVideoTrack = null
                                    activeRemoteParticipant = null
                                }
                            }
                            else -> {}
                        }
                    }
                }

                connectionState = Room.State.CONNECTING
                activeRoom.connect(serverUrl, token)

                // Enforce Local Media Publishing
                val localParticipant = activeRoom.localParticipant
                publishLocalTracks(localParticipant)

            } catch (e: Exception) {
                connectionState = Room.State.DISCONNECTED
                onError(e.message ?: "Unknown connection error")
            }
        }
    }

    /**
     * Publishes camera and audio tracks, specifically forcing HEVC (H.265) encoding.
     */
    private suspend fun publishLocalTracks(localParticipant: LocalParticipant) {
        // 1. Audio track
        val audioTrack = localParticipant.createAudioTrack()
        audioTrack.start()
        localParticipant.publishAudioTrack(audioTrack)
        localAudioTrack = audioTrack

        // 2. Video track with H.265 hardware codec instruction
        val videoTrack = localParticipant.createVideoTrack()
        videoTrack.startCapture()
        localParticipant.publishVideoTrack(
            track = videoTrack,
            options = VideoTrackPublishOptions(
                videoCodec = "h265" // Forced HEVC hardware instruction
            )
        )
        localVideoTrack = videoTrack
    }

    /**
     * Toggle Mute/Unmute state of local audio.
     */
    fun toggleMute() {
        val track = localAudioTrack ?: return
        isMuted = !isMuted
        track.enabled = !isMuted
    }

    /**
     * Toggle Local Camera Active state.
     */
    fun toggleCamera() {
        val track = localVideoTrack ?: return
        isCameraEnabled = !isCameraEnabled
        track.enabled = isCameraEnabled
    }

    /**
     * Toggle Screen Sharing state.
     */
    suspend fun toggleScreenShare(enabled: Boolean, resultData: Intent? = null) {
        val localParticipant = room?.localParticipant ?: return
        if (enabled) {
            if (resultData == null) return
            localParticipant.setScreenShareEnabled(true, resultData)
            isScreenShareEnabled = true
        } else {
            localParticipant.setScreenShareEnabled(false)
            isScreenShareEnabled = false
        }
    }

    /**
     * Disconnects active call room.
     */
    fun disconnect() {
        scope.launch {
            try {
                room?.disconnect()
            } catch (e: Exception) {
                // Ignore disconnect exceptions during cleanup
            } finally {
                cleanUp()
            }
        }
    }

    private fun cleanUp() {
        localVideoTrack = null
        remoteVideoTrack = null
        localAudioTrack = null
        activeRemoteParticipant = null
        room = null
        connectionState = Room.State.DISCONNECTED
        isScreenShareEnabled = false
    }
}
