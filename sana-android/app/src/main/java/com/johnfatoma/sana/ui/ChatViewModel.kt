package com.johnfatoma.sana.ui

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.johnfatoma.sana.SanaApplication
import com.johnfatoma.sana.actions.ActionReporter
import com.johnfatoma.sana.actions.SanaAction
import com.johnfatoma.sana.auth.SupabaseClientProvider
import com.johnfatoma.sana.live.LiveVoiceClient
import com.johnfatoma.sana.live.LiveVoiceState
import com.johnfatoma.sana.network.ChatApiClient
import kotlinx.coroutines.launch

data class ChatMessage(
    val role: String, // "user" or "assistant"
    val content: String,
)

/**
 * Sana AI — Chat ViewModel
 *
 * The one place that ties together everything built across every
 * previous phase:
 *   - ChatApiClient  (typed /chat requests)
 *   - LiveVoiceClient (real-time voice via /live/ws)
 *   - ActionDispatcher (executing create_timer, etc. on-device)
 *   - ActionReporter  (reporting the result back to the backend)
 *
 * Both the typed-chat path and the voice path funnel through the
 * same handleAction() function, so a timer requested by typing
 * "set a 20 minute timer" and one requested by voice behave
 * identically on-device.
 */
class ChatViewModel(
    application: Application,
    private val backendBaseUrl: String, // e.g. "https://your-app.onrender.com"
) : AndroidViewModel(application) {

    private val backendWsUrl: String =
        backendBaseUrl.replace("https://", "wss://").replace("http://", "ws://") + "/live/ws"

    private val chatApiClient = ChatApiClient(baseUrl = backendBaseUrl)
    private val actionReporter = ActionReporter(baseUrl = backendBaseUrl)

    private val dispatcher = (application as SanaApplication).dispatcher

    val messages: SnapshotStateList<ChatMessage> =
        androidx.compose.runtime.mutableStateListOf()

    val isSending = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val voiceState = mutableStateOf(LiveVoiceState.IDLE)

    private var conversationId: String? = null
    private var liveVoiceClient: LiveVoiceClient? = null

    // ------------------------------------------------------------
    // Typed chat
    // ------------------------------------------------------------

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || isSending.value) return

        val accessToken = SupabaseClientProvider.currentAccessToken()
        if (accessToken == null) {
            errorMessage.value = "You're signed out — please sign in again."
            return
        }

        messages.add(ChatMessage(role = "user", content = trimmed))
        isSending.value = true
        errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = chatApiClient.sendMessage(
                    accessToken = accessToken,
                    message = trimmed,
                    conversationId = conversationId,
                )

                conversationId = result.conversationId
                messages.add(ChatMessage(role = "assistant", content = result.message))

                result.action?.let { action ->
                    handleAction(action, accessToken)
                }

            } catch (e: ChatApiClient.ChatError) {
                errorMessage.value = when (e) {
                    is ChatApiClient.ChatError.Network -> "Couldn't reach Sana — check your connection."
                    is ChatApiClient.ChatError.Server -> e.message
                }
            } catch (e: Exception) {
                errorMessage.value = "Something went wrong: ${e.message}"
            } finally {
                isSending.value = false
            }
        }
    }

    // ------------------------------------------------------------
    // Live voice
    // ------------------------------------------------------------

    fun startVoiceSession() {
        val accessToken = SupabaseClientProvider.currentAccessToken()
        if (accessToken == null) {
            errorMessage.value = "You're signed out — please sign in again."
            return
        }

        if (liveVoiceClient != null) return // already running

        liveVoiceClient = LiveVoiceClient(
            backendWsUrl = backendWsUrl,
            accessToken = accessToken,
            conversationId = conversationId,
            onStateChange = { state -> voiceState.value = state },
            onAction = { action ->
                viewModelScope.launch {
                    handleAction(action, accessToken)
                }
            },
            onError = { message -> errorMessage.value = message },
        )

        liveVoiceClient?.start()
    }

    fun stopVoiceSession() {
        liveVoiceClient?.stop()
        liveVoiceClient = null
        voiceState.value = LiveVoiceState.IDLE
    }

    fun endVoiceTurn() {
        liveVoiceClient?.signalEndOfTurn()
    }

    // ------------------------------------------------------------
    // Shared action handling (used by BOTH chat and voice)
    // ------------------------------------------------------------

    private suspend fun handleAction(action: SanaAction, accessToken: String) {
        val result = dispatcher.dispatch(action)
        actionReporter.report(accessToken, result)
    }

    override fun onCleared() {
        super.onCleared()
        liveVoiceClient?.stop()
    }

    companion object {
        /**
         * ChatViewModel needs backendBaseUrl in its constructor, which
         * the default ViewModel factory can't provide — use this in
         * your composable: viewModel(factory = ChatViewModel.factory(app, url))
         */
        fun factory(
            application: Application,
            backendBaseUrl: String,
        ): androidx.lifecycle.ViewModelProvider.Factory =
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return ChatViewModel(application, backendBaseUrl) as T
                }
            }
    }
}
