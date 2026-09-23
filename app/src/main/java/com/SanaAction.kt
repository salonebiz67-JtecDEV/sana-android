package com.johnfatoma.sana.actions

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
)

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
