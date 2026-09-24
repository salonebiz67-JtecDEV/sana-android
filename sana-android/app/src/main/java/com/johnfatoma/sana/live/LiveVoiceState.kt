package com.johnfatoma.sana.live

/**
 * Mirrors the voice state diagram from earlier planning:
 * IDLE -> LISTENING -> THINKING -> SPEAKING -> IDLE
 *
 * With Gemini Live, THINKING and SPEAKING often overlap slightly
 * (audio can start streaming back before the "turn" is fully done),
 * so treat SPEAKING as "audio is currently playing" rather than a
 * strict sequential phase.
 */
enum class LiveVoiceState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR,
}
