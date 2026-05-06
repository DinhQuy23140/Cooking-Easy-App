package com.example.cookingeasy.call

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cookingeasy.data.remote.api.CallActionRequest
import com.example.cookingeasy.data.remote.api.InitiateCallRequest
import com.example.cookingeasy.data.remote.api.LaravelClient
import com.example.cookingeasy.databinding.ActivityInCallBinding
import com.example.cookingeasy.ui.main.MainActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import java.util.concurrent.atomic.AtomicBoolean

class InCallActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInCallBinding
    private lateinit var callRepo: CallSignalingRepository
    private lateinit var webRtcManager: WebRtcCallManager
    private var signalingJob: Job? = null
    private var statusJob: Job? = null
    private val isClosing = AtomicBoolean(false)
    private val isShutdown = AtomicBoolean(false)
    private val processedSignalIds = linkedSetOf<String>()
    private val addedCandidateKeys = linkedSetOf<String>()
    private var remoteOfferSet = false
    private var remoteAnswerSet = false

    private var callId: String = ""
    private var peerId: String = ""
    private var type: String = "audio"
    private var isCaller: Boolean = false
    private val myUid: String by lazy { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        callRepo = CallSignalingRepository()
        webRtcManager = WebRtcCallManager(this)

        callId = intent.getStringExtra(IncomingCallActivity.EXTRA_CALL_ID).orEmpty()
        peerId = intent.getStringExtra(IncomingCallActivity.EXTRA_PEER_ID).orEmpty()
        type = intent.getStringExtra(IncomingCallActivity.EXTRA_CALL_TYPE).orEmpty().ifBlank { "audio" }
        isCaller = intent.getBooleanExtra(EXTRA_IS_CALLER, false)

        lifecycleScope.launch {
            if (!initializeCallSession()) return@launch
            startCallForegroundService()
            setupPeerConnection()
            subscribeSignaling()
            subscribeCallStatus()
        }

        binding.btnEndCall.setOnClickListener { endCall() }
    }

    private suspend fun initializeCallSession(): Boolean {
        if (myUid.isBlank()) {
            closeCallScreen()
            return false
        }

        if (isCaller) {
            if (peerId.isBlank()) {
                closeCallScreen()
                return false
            }
            if (callId.isBlank()) {
                val response = LaravelClient.api.initiateCall(InitiateCallRequest(myUid, peerId, type))
                callId = response.callId.orEmpty()
            }
        }

        if (callId.isBlank()) {
            closeCallScreen()
            return false
        }

        return true
    }

    private fun setupPeerConnection() {
        webRtcManager.createPeerConnection(
            isVideo = type == "video",
            onIceCandidate = { candidate ->
                lifecycleScope.launch {
                    callRepo.sendCandidate(
                        callId = callId,
                        senderId = myUid,
                        targetId = peerId,
                        candidate = candidate.sdp,
                        sdpMid = candidate.sdpMid.orEmpty(),
                        sdpMLineIndex = candidate.sdpMLineIndex
                    )
                }
            },
            onConnectionStateChanged = { state ->
                binding.txtStatus.text = "ICE: $state"
            }
        )

        if (isCaller) {
            webRtcManager.createOffer { offer ->
                lifecycleScope.launch {
                    callRepo.sendOffer(callId, myUid, peerId, offer.description)
                }
            }
        }
    }

    private fun subscribeSignaling() {
        signalingJob?.cancel()
        signalingJob = lifecycleScope.launch {
            callRepo.listenSignaling(callId).collectLatest { signals ->
                signals.forEach { msg ->
                    if (msg.id.isBlank() || processedSignalIds.contains(msg.id)) return@forEach
                    if (msg.senderId == myUid) return@forEach
                    if (msg.targetId.isNotBlank() && msg.targetId != myUid) return@forEach

                    processedSignalIds.add(msg.id)
                    when (msg.type) {
                        "offer" -> {
                            if (remoteOfferSet) return@forEach
                            webRtcManager.setRemoteDescription("offer", msg.sdp)
                            remoteOfferSet = true
                            webRtcManager.createAnswer { answer ->
                                lifecycleScope.launch {
                                    callRepo.sendAnswer(callId, myUid, peerId, answer.description)
                                }
                            }
                        }

                        "answer" -> {
                            if (remoteAnswerSet) return@forEach
                            webRtcManager.setRemoteDescription("answer", msg.sdp)
                            remoteAnswerSet = true
                        }
                        "candidate" -> {
                            val key = "${msg.sdpMid}:${msg.sdpMLineIndex}:${msg.candidate}"
                            if (!addedCandidateKeys.add(key)) return@forEach
                            webRtcManager.addIceCandidate(
                                IceCandidate(msg.sdpMid, msg.sdpMLineIndex, msg.candidate)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun subscribeCallStatus() {
        statusJob?.cancel()
        statusJob = lifecycleScope.launch {
            callRepo.listenCall(callId).collectLatest { call ->
                if (call == null) return@collectLatest
                binding.txtStatus.text = "Status: ${call.status}"
                if (call.status in setOf("ended", "rejected", "timeout")) {
                    closeCallScreen()
                }
            }
        }
    }

    private fun endCall() {
        lifecycleScope.launch {
            runCatching { LaravelClient.api.endCall(CallActionRequest(callId)) }
            closeCallScreen()
        }
    }

    private fun closeCallScreen() {
        if (!isClosing.compareAndSet(false, true)) return

        shutdownRtc()

        if (isTaskRoot) {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
        }
        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 120L)
    }

    private fun shutdownRtc() {
        if (!isShutdown.compareAndSet(false, true)) return
        signalingJob?.cancel()
        statusJob?.cancel()
        webRtcManager.release()
        stopService(Intent(this, CallForegroundService::class.java))
    }

    private fun startCallForegroundService() {
        runCatching {
            val intent = Intent(this, CallForegroundService::class.java).apply {
                putExtra(CallForegroundService.EXTRA_LABEL, "In call with $peerId")
            }
            ContextCompat.startForegroundService(this, intent)
        }.onFailure {
            Log.e("InCallActivity", "Unable to start call foreground service", it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        shutdownRtc()
        Log.d("InCallActivity", "Resources cleaned")
    }

    companion object {
        const val EXTRA_IS_CALLER = "extra_is_caller"
    }
}
