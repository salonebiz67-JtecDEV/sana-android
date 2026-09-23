package com.johnfatoma.sana

import android.app.Application
import com.johnfatoma.sana.actions.ActionDispatcher
import com.johnfatoma.sana.actions.TimerActionExecutor
import com.johnfatoma.sana.actions.setupActionDispatcher

/**
 * Sana AI — Application
 *
 * Sets up the ActionDispatcher ONCE here, at app startup, and every
 * screen shares this same instance via SanaApp.dispatcher.
 *
 * IMPORTANT: if you add a new action type/executor (the Android
 * equivalent of a backend tool), register it here. This is the
 * single place that must not be forgotten — it's the direct
 * counterpart to the backend bug we found twice already
 * (tools_timer.py not imported, and three API routers not
 * registered in main.py). Don't make it a third time.
 */
class SanaApplication : Application() {

    lateinit var dispatcher: ActionDispatcher
        private set

    override fun onCreate() {
        super.onCreate()

        dispatcher = setupActionDispatcher(
            timerExecutor = TimerActionExecutor(applicationContext),
            // Register future executors in setupActionDispatcher()
            // (app/src/main/java/.../actions/ActionDispatcher.kt),
            // then pass them here too.
        )
    }
}

