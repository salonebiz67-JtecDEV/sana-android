package com.johnfatoma.sana.live

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import com.johnfatoma.sana.actions.SanaAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject

/**
 * Sana AI — Live Voice Client
 *
 * Streams microphone audio to the backend's /live/ws endpoint and
 * plays back Sana's spoken responses in real time. This is the
 * "always-on conversation" voice mode (Gemini Live), distinct from
 * the earlier one-shot wake-word -> STT -> chat -> TTS flow: here,
 * audio flows continuously in both directions over one connection.
 *
 * Audio contract (fixed by the backend/Gemini Live, do not change):
 *   - Mic input sent to backend:  16-bit PCM, mono, 16kHz
 *   - Playback audio from backend: 16-bit PCM, mono, 24kHz
 */
class LiveVoiceClient(
    private val backendWsUrl: String,   // e.g. "wss://your-app.onrender.com/live/ws"
    private val accessToken: String,
    private val conversationId: String? = null,
    private val onStateChange: (LiveVoiceState) -> Unit = {},
    private val onAction: (SanaAction) -> Unit = {},
    private val onError: (String) -> Unit = {},
) {
    companion object {
        private const val INPUT_SAMPLE_RATE = 16000
        private const val OUTPUT_SAMPLE_RATE = 24000
    }

    private val client = OkHttpClient.Builder().build()
    private var webSocket: WebSocket? = null
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var recordJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isRunning = false

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start() {
        if (isRunning) return
        isRunning = true

        val url = buildString {
            append(backendWsUrl)
            if (conversationId != null) {
                append("?conversation_id=$conversationId")
            }
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        onStateChange(LiveVoiceState.LISTENING)

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                setupAudioTrack()
                startMicCapture(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // Raw PCM audio from Gemini -> play it
                onStateChange(LiveVoiceState.SPEAKING)
                playAudioChunk(bytes.toByteArray())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleControlMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onStateChange(LiveVoiceState.ERROR)
                onError(t.message ?: "WebSocket connection failed.")
                stop()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onStateChange(LiveVoiceState.IDLE)
            }
        })
    }

    fun stop() {
        isRunning = false
        recordJob?.cancel()
        recordJob = null

        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null

        audioTrack?.apply {
            stop()
            release()
        }
        audioTrack = null

        webSocket?.close(1000, "Client stopped")
        webSocket = null

        onStateChange(LiveVoiceState.IDLE)
    }

    // --------------------------------------------------------------
    // Microphone capture -> WebSocket
    // --------------------------------------------------------------

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startMicCapture(socket: WebSocket) {
        val minBufferSize = AudioRecord.getMinBufferSize(
            INPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )

        if (minBufferSize <= 0) {
            onError("This device does not support the required audio input format.")
            return
        }

        val bufferSize = minBufferSize * 2

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            INPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        )

        audioRecord?.startRecording()

        recordJob = scope.launch {
            val buffer = ByteArray(bufferSize)
            while (isRunning) {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                if (bytesRead > 0) {
                    socket.send(ByteString.of(*buffer.copyOf(bytesRead)))
                }
            }
        }
    }

    // --------------------------------------------------------------
    // WebSocket -> playback
    // --------------------------------------------------------------

    private fun setupAudioTrack() {
        val minBufferSize = AudioTrack.getMinBufferSize(
            OUTPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(OUTPUT_SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
    }

    private fun playAudioChunk(chunk: ByteArray) {
        audioTrack?.write(chunk, 0, chunk.size)
    }

    // --------------------------------------------------------------
    // Control messages (actions, errors) from the backend
    // --------------------------------------------------------------

    private fun handleControlMessage(text: String) {
        try {
            val json = JSONObject(text)
            when (json.optString("type")) {
                "action" -> {
                    val actionJson = json.getJSONObject("action")
                    onAction(SanaAction.fromJson(actionJson))
                }
                "error" -> {
                    onError(json.optString("message", "Unknown error from server."))
                }
            }
        } catch (e: Exception) {
            onError("Failed to parse server message: ${e.message}")
        }
    }

    /**
     * Call when the user has finished speaking (e.g. releasing a
     * push-to-talk button), if you're not relying purely on Gemini's
     * own voice-activity detection to know when a turn ends.
     */
    fun signalEndOfTurn() {
        webSocket?.send("""{"type": "end_turn"}""")
    }
}
