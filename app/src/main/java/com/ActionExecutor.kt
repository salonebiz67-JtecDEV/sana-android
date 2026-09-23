package com.johnfatoma.sana.actions

/**
 * Sana AI — Action Executor
 *
 * Every action type Sana can request (create_timer, and later
 * things like create_reminder, open_app, etc.) implements this.
 *
 * This mirrors the backend's SanaTool pattern: the backend decides
 * WHAT should happen, the Android client decides HOW it actually
 * happens safely on-device (permissions, system APIs, etc.).
 */
interface ActionExecutor {
    /** The action "type" string this executor handles, e.g. "create_timer" */
    val type: String

    /** Execute the action and report what actually happened. */
    suspend fun execute(action: SanaAction): SanaActionResult
}
