package com.johnfatoma.sana.actions

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Sana AI — Action Reporter
 *
 * Sends the outcome of an executed action back to the backend's
 * POST /actions/result endpoint (app/api/actions.py), so Sana knows
 * whether the timer/reminder/etc. actually happened on-device.
 *
 * Uses OkHttp directly to keep this file dependency-light and easy
 * to drop into whatever networking setup you already have. If your
 * project uses Retrofit, wrap this same JSON body in a @POST call
 * on your existing API interface instead.
 */
class ActionReporter(
    private val baseUrl: String, // e.g. "https://your-app.onrender.com"
    private val client: OkHttpClient = OkHttpClient(),
) {

    /**
     * @param accessToken the Supabase auth token for the current user,
     *   sent exactly the same way your other authenticated requests
     *   already send it (Authorization: Bearer <token>).
     */
    fun report(accessToken: String, result: SanaActionResult) {
        val json = JSONObject().apply {
            put("action_type", result.actionType)
            put("success", result.success)
            put("message", result.message)
            put("data", JSONObject(result.data))
        }

        val body = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/actions/result")
            .addHeader("Authorization", "Bearer $accessToken")
            .post(body)
            .build()

        // Fire-and-forget: a failed report shouldn't crash the app or
        // block the user from seeing that their timer was already set.
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                // TODO: hook into your logging setup
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.close()
            }
        })
    }
}
