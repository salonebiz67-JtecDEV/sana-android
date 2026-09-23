package com.johnfatoma.sana.actions

/**
 * Sana AI — Action Dispatcher
 *
 * Registry of every ActionExecutor available on this device.
 * Mirrors the backend's ToolRegistry (app/ai/tools.py) so both
 * sides follow the same "register once, dispatch by type name"
 * pattern.
 *
 * IMPORTANT: if you add a new action type here, you MUST also
 * register it in setupActionDispatcher() below, or it will silently
 * fail with an "unknown action" result — exactly like the backend's
 * tools_timer.py bug we found (a tool defined but never imported).
 */
class ActionDispatcher {
    private val executors = mutableMapOf<String, ActionExecutor>()

    fun register(executor: ActionExecutor) {
        executors[executor.type] = executor
    }

    suspend fun dispatch(action: SanaAction): SanaActionResult {
        val executor = executors[action.type]
            ?: return SanaActionResult(
                actionType = action.type,
                success = false,
                message = "No executor registered for action type '${action.type}'.",
            )

        return try {
            executor.execute(action)
        } catch (e: Exception) {
            SanaActionResult(
                actionType = action.type,
                success = false,
                message = "Action failed: ${e.message}",
            )
        }
    }
}

/**
 * Call this once at app startup (e.g. in your Application class)
 * and reuse the same dispatcher instance everywhere.
 */
fun setupActionDispatcher(timerExecutor: TimerActionExecutor): ActionDispatcher {
    val dispatcher = ActionDispatcher()
    dispatcher.register(timerExecutor)
    // Register future executors here as they're built, e.g.:
    // dispatcher.register(reminderExecutor)
    return dispatcher
}
