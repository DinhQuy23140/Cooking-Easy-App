package com.example.cookingeasy.call

import android.content.Context
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CandidatePairChangeEvent
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import java.util.concurrent.atomic.AtomicBoolean

class WebRtcCallManager(private val context: Context) {
    private val eglBase: EglBase = EglBase.create()
    private val peerFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private var videoTrack: VideoTrack? = null
    private var audioTrack: AudioTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private val isReleased = AtomicBoolean(false)

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
        )
        peerFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
    }

    fun createPeerConnection(
        isVideo: Boolean,
        onIceCandidate: (IceCandidate) -> Unit,
        onConnectionStateChanged: (PeerConnection.IceConnectionState) -> Unit
    ): PeerConnection {
        check(!isReleased.get()) { "WebRtcCallManager already released" }

        val rtcConfig = PeerConnection.RTCConfiguration(
            listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
            )
        )

        peerConnection = peerFactory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) = onIceCandidate(candidate)
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                onConnectionStateChanged(newState ?: PeerConnection.IceConnectionState.NEW)
            }
            override fun onSignalingChange(newState: PeerConnection.SignalingState?) = Unit
            override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) = Unit
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit
            override fun onAddStream(stream: org.webrtc.MediaStream?) = Unit
            override fun onRemoveStream(stream: org.webrtc.MediaStream?) = Unit
            override fun onDataChannel(dc: org.webrtc.DataChannel?) = Unit
            override fun onRenegotiationNeeded() = Unit
            override fun onAddTrack(receiver: org.webrtc.RtpReceiver?, mediaStreams: Array<out org.webrtc.MediaStream>?) = Unit
            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) = Unit
            override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent?) = Unit
        }) ?: error("Failed to create PeerConnection")

        attachLocalTracks(isVideo)
        return peerConnection!!
    }

    fun createOffer(onOfferReady: (SessionDescription) -> Unit) {
        peerConnection?.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                if (sdp == null) return
                peerConnection?.setLocalDescription(SdpObserverAdapter(), sdp)
                onOfferReady(sdp)
            }
        }, MediaConstraints())
    }

    fun createAnswer(onAnswerReady: (SessionDescription) -> Unit) {
        peerConnection?.createAnswer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                if (sdp == null) return
                peerConnection?.setLocalDescription(SdpObserverAdapter(), sdp)
                onAnswerReady(sdp)
            }
        }, MediaConstraints())
    }

    fun setRemoteDescription(type: String, sdp: String) {
        val sdpType = if (type == "offer") SessionDescription.Type.OFFER else SessionDescription.Type.ANSWER
        peerConnection?.setRemoteDescription(SdpObserverAdapter(), SessionDescription(sdpType, sdp))
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    fun release() {
        if (!isReleased.compareAndSet(false, true)) return

        runCatching { peerConnection?.close() }
        // Ultra-safe demo mode: avoid direct capturer stop/dispose calls.
        // On some devices these trigger fatal JNI races on signaling thread.
        videoCapturer = null

        runCatching { videoTrack?.setEnabled(false) }
        runCatching { audioTrack?.setEnabled(false) }
        // Skip explicit track dispose in demo stability mode; close() + nulling references
        // is less crash-prone on some OEM builds.
        videoTrack = null
        audioTrack = null

        // Skip explicit source dispose in demo stability mode.
        videoSource = null
        audioSource = null

        // Skip explicit peerConnection.dispose() in demo stability mode.
        // close() is enough for call teardown and avoids JNI race crashes.
        peerConnection = null

        // Skip explicit surfaceTextureHelper.dispose() in demo stability mode.
        surfaceTextureHelper = null

        // Intentionally skip peerFactory.dispose() and eglBase.release() here.
        // They can trigger fatal JNI shutdown races on certain devices/ROMs.
    }

    private fun attachLocalTracks(isVideo: Boolean) {
        audioSource = peerFactory.createAudioSource(MediaConstraints())
        audioTrack = peerFactory.createAudioTrack("audio-track", audioSource ?: return)
        peerConnection?.addTrack(audioTrack)

        if (!isVideo) return
        videoCapturer = createVideoCapturer() ?: return
        videoSource = peerFactory.createVideoSource(false)
        surfaceTextureHelper = SurfaceTextureHelper.create("capture", eglBase.eglBaseContext)
        videoCapturer?.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
        videoCapturer?.startCapture(720, 1280, 30)
        val source = videoSource ?: return
        videoTrack = peerFactory.createVideoTrack("video-track", source)
        peerConnection?.addTrack(videoTrack)
    }

    private fun createVideoCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        enumerator.deviceNames.forEach { name ->
            if (enumerator.isFrontFacing(name)) return enumerator.createCapturer(name, null)
        }
        return null
    }
}
