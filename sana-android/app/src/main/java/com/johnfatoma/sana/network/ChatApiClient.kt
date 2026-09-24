package com.johnfatoma.sana.network

import com.johnfatoma.sana.actions.SanaAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * Sana AI — Chat API Client
 *
 * Talks to the backend's POST /chat endpoint (app/main.py).
 * Kept as plain OkHttp + org.json, matching ActionReporter's style,
 * so the whole app doesn't need Retrofit/Moshi/Gson just for two
 * simple JSON endpoints.
 */
class ChatApiClient(
    private val baseUrl: String, // e.g. "https://your-app.onrender.com"
    private val client: OkHttpClient = OkHttpClient(),
) {

    data class ChatResult(
        val message: String,
        val conversationId: String,
        val action: SanaAction?,
    )

    sealed class ChatError : Exception() {
        data class Network(override val message: String) : ChatError()
        data class Server(val statusCode: Int, override val message: String) : ChatError()
    }

    /**
     * Send a message to Sana. Suspends until the reply arrives —
     * call this from a coroutine (e.g. inside a ViewModel's
     * viewModelScope.launch).
     */
    suspend fun sendMessage(
        accessToken: String,
        message: String,
        conversationId: String? = null,
    ): ChatResult = withContext(Dispatchers.IO) {

        val requestBody = JSONObject().apply {
            put("message", message)
            if (conversationId != null) {
                put("conversation_id", conversationId)
            }
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/chat")
            .addHeader("Authorization", "Bearer $accessToken")
            .post(requestBody)
            .build()

        val response = try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            throw ChatError.Network(e.message ?: "Network request failed.")
        }

        response.use {
            val bodyString = it.body?.string().orEmpty()

            if (!it.isSuccessful) {
                val detail = try {
                    JSONObject(bodyString).optString("detail", "Request failed.")
                } catch (e: Exception) {
                    "Request failed with status ${it.code}."
                }
                throw ChatError.Server(it.code, detail)
            }

            val json = JSONObject(bodyString)

            val action = json.optJSONObject("action")?.let { actionJson ->
                SanaAction.fromJson(actionJson)
            }

            ChatResult(
                message = json.getString("message"),
                conversationId = json.getString("conversation_id"),
                action = action,
            )
        }
    }
}
