package com.example.cookingeasy.call

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cookingeasy.call.InCallActivity.Companion.EXTRA_IS_CALLER
import com.example.cookingeasy.data.remote.api.CallActionRequest
import com.example.cookingeasy.data.remote.api.LaravelClient
import com.example.cookingeasy.databinding.ActivityIncomingCallBinding
import kotlinx.coroutines.launch

class IncomingCallActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIncomingCallBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncomingCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val callId = intent.getStringExtra(EXTRA_CALL_ID).orEmpty()
        val callerName = intent.getStringExtra(EXTRA_CALLER_NAME).orEmpty()
        val callerId = intent.getStringExtra(EXTRA_CALLER_ID).orEmpty()
        val callType = intent.getStringExtra(EXTRA_CALL_TYPE).orEmpty()

        binding.txtCaller.text = if (callerName.isBlank()) "Incoming call" else callerName
        binding.txtType.text = callType.ifBlank { "audio" }

        binding.btnAccept.setOnClickListener {
            lifecycleScope.launch {
                runCatching {
                    LaravelClient.api.acceptCall(CallActionRequest(callId))
                }
                startActivity(Intent(this@IncomingCallActivity, InCallActivity::class.java).apply {
                    putExtra(EXTRA_CALL_ID, callId)
                    putExtra(EXTRA_PEER_ID, callerId)
                    putExtra(EXTRA_CALL_TYPE, callType)
                    putExtra(EXTRA_IS_CALLER, false)
                })
                finish()
            }
        }

        binding.btnReject.setOnClickListener {
            lifecycleScope.launch {
                runCatching { LaravelClient.api.rejectCall(CallActionRequest(callId)) }
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_CALL_ID = "extra_call_id"
        const val EXTRA_CALLER_ID = "extra_caller_id"
        const val EXTRA_CALLER_NAME = "extra_caller_name"
        const val EXTRA_CALL_TYPE = "extra_call_type"
        const val EXTRA_PEER_ID = "extra_peer_id"
    }
}
