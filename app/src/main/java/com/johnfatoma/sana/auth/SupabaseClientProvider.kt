package com.johnfatoma.sana.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient

/**
 * Sana AI — Supabase Client
 *
 * One shared Supabase client for the whole app, with the Auth
 * plugin installed so it can hold the current session and hand out
 * fresh access tokens to LiveVoiceClient, ActionReporter, and your
 * chat networking code.
 *
 * IMPORTANT: use the anon/public key here, NEVER the service_role
 * key. The service_role key belongs only on your backend
 * (app/core/config.py) — putting it in the Android app would let
 * anyone who decompiles the APK bypass every RLS policy.
 */
object SupabaseClientProvider {

    // TODO: replace with your actual Supabase project values
    // (Project Settings -> API in the Supabase dashboard).
    private const val SUPABASE_URL = "https://your-project.supabase.co"
    private const val SUPABASE_ANON_KEY = "your-anon-public-key-here"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY,
        ) {
            install(Auth)
        }
    }

    /** The current user's access token, or null if not signed in. */
    fun currentAccessToken(): String? {
        return client.auth.currentAccessTokenOrNull()
    }

    /** The current user's id, or null if not signed in. */
    fun currentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }
}
