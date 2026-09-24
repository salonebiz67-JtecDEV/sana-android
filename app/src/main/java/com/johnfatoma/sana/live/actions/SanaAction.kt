package com.johnfatoma.sana.actions

import org.json.JSONObject

/**
 * Sana AI — Action Contract (Android side)
 *
 * Mirrors the backend's SanaAction model exactly (app/ai/actions.py).
 * This is what arrives inside the /chat response whenever the AI
 * brain requests something the Android device needs to actually do.
 */
data class SanaAction(
    val type: String,
    val requiresConfirmation: Boolean = false,
    val parameters: Map<String, Any?> = emptyMap(),
) {
    companion object {
        /**
         * Parses a SanaAction from JSON shaped like the backend sends it:
         * { "type": "create_timer", "parameters": { "duration_seconds": 1200, "label": "Tea" } }
         *
         * Shared by ChatApiClient (/chat response) and LiveVoiceClient
         * (voice session control messages) so both parse it identically.
         */
        fun fromJson(json: JSONObject): SanaAction {
            val parametersJson = json.optJSONObject("parameters")
            val parameters = mutableMapOf<String, Any?>()
            parametersJson?.keys()?.forEach { key ->
                parameters[key] = parametersJson.get(key)
            }

            return SanaAction(
                type = json.getString("type"),
                requiresConfirmation = json.optBoolean("requires_confirmation", false),
                parameters = parameters,
            )
        }
    }
}

/**
 * Mirrors the backend's SanaActionResult model. This is what gets
 * sent back to POST /actions/result after the device attempts
 * to execute an action.
 */
data class SanaActionResult(
    val actionType: String,
    val success: Boolean,
    val message: String = "",
    val data: Map<String, Any?> = emptyMap(),
)

