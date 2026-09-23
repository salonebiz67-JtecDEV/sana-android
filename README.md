# Sana — Android Action System

This is the Android-side counterpart to the backend's action-contract
system (`app/ai/actions.py`, `app/ai/tools.py`). The backend never
touches your device directly — it returns a `SanaAction` in the
`/chat` response, and this code decides how to execute it safely.

## How it fits together

```
/chat response
      │
      ▼
  action: { type: "create_timer", parameters: {...} }
      │
      ▼
ActionDispatcher.dispatch(action)
      │
      ▼
TimerActionExecutor.execute(action)   <- uses AlarmManager
      │
      ▼
SanaActionResult (success/failure)
      │
      ▼
ActionReporter.report(...)  ──▶  POST /actions/result on the backend
```

## Files in this module

- `SanaAction.kt` — data classes mirroring the backend's Pydantic models exactly
- `ActionExecutor.kt` — interface every action type implements
- `ActionDispatcher.kt` — registry routing an action's `type` string to its executor (mirrors the backend's `ToolRegistry`)
- `TimerActionExecutor.kt` — real implementation for `create_timer`, using `AlarmManager`
- `TimerAlarmReceiver.kt` — fires when the timer completes; shows a notification AND speaks the label aloud via TTS
- `ActionReporter.kt` — reports the result back to the backend

## Required additions to `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<application ...>
    <receiver
        android:name=".actions.TimerAlarmReceiver"
        android:exported="false" />
</application>
```

- `POST_NOTIFICATIONS` is a **runtime** permission on Android 13+ — you must request it from the user, this manifest entry alone isn't enough.
- `SCHEDULE_EXACT_ALARM` on Android 12+ (API 31+) may need the user to approve it in system settings, depending on your app's target category. Test this on a real device early — exact-alarm restrictions vary by Android version and this is the single most likely thing to silently fail if skipped.

## Required Gradle dependency

For `ActionReporter.kt` (OkHttp):

```kotlin
implementation("com.squareup.okhttp3:okhttp:4.12.0")
```

## Wiring it up (in your Application class or main activity)

```kotlin
val dispatcher = setupActionDispatcher(
    timerExecutor = TimerActionExecutor(applicationContext)
)

// After receiving a /chat response with a non-null `action`:
val result = dispatcher.dispatch(action)
actionReporter.report(accessToken, result)
```

## Honest gaps to know about

- Only `create_timer` is implemented. Any other action `type` the backend ever sends (e.g. a future `create_reminder` action) will hit the "No executor registered" fallback in `ActionDispatcher` until you build and register an executor for it — same class of bug we found and fixed in the backend's `tools_timer.py`.
- `TimerAlarmReceiver` creates a new `TextToSpeech` instance per alarm. Fine for occasional timers; if Sana ends up speaking frequently, move to a single app-wide TTS instance instead.
- The notification's tap action is a placeholder empty `Intent()` — wire it to your actual main activity once one exists.
