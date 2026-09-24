package com.johnfatoma.sana.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import java.security.MessageDigest
import java.util.UUID

/**
 * Sana AI — Google Sign-In
 *
 * "Continue with Google" — one tap, no email/password screen.
 *
 * Flow:
 *   1. Ask Android's Credential Manager for a Google ID token
 *      (this shows the OS-native account picker)
 *   2. Hand that ID token to Supabase Auth, which verifies it with
 *      Google and creates/logs into the matching Supabase user
 *   3. From here on, SupabaseClientProvider.currentAccessToken()
 *      is what you pass to your backend (/chat, /actions/result,
 *      /live/ws, etc.) — exactly the same as any other sign-in
 *      method, since the backend only ever sees a Supabase token.
 *
 * SETUP REQUIRED before this works:
 *   - Google Cloud Console: create a Web-type OAuth Client ID
 *     (this is the "server client id" below) AND an Android-type
 *     OAuth Client ID (package name + SHA-1 fingerprint) — Android
 *     needs both to exist, even though only the Web one is used
 *     directly in this code.
 *   - Supabase Dashboard -> Authentication -> Providers -> Google:
 *     paste in the Web Client ID + its secret, enable the provider.
 */
class GoogleSignInManager(
    private val context: Context,
    private val webClientId: String, // the Web-type OAuth Client ID from Google Cloud Console
) {

    sealed class SignInResult {
        data class Success(val userId: String) : SignInResult()
        data class Failure(val message: String) : SignInResult()
        object Cancelled : SignInResult()
    }

    suspend fun signIn(): SignInResult {
        val credentialManager = CredentialManager.create(context)
        val rawNonce = generateRawNonce()
        val hashedNonce = sha256(rawNonce)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            // false = show every Google account on the device, not just
            // ones previously used with this app. Needed for first-time
            // sign-up, not just returning sign-in.
            .setFilterByAuthorizedAccounts(false)
            .setNonce(hashedNonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val idToken: String

        try {
            val result = credentialManager.getCredential(
                request = request,
                context = context,
            )

            val credential = result.credential

            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                return SignInResult.Failure("Unexpected credential type from Google.")
            }

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            idToken = googleIdTokenCredential.idToken

        } catch (e: GetCredentialCancellationException) {
            return SignInResult.Cancelled
        } catch (e: NoCredentialException) {
            return SignInResult.Failure("No Google account found on this device.")
        } catch (e: GoogleIdTokenParsingException) {
            return SignInResult.Failure("Could not read the Google credential.")
        } catch (e: GetCredentialException) {
            return SignInResult.Failure(e.message ?: "Google sign-in failed.")
        }

        return try {
            SupabaseClientProvider.client.auth.signInWith(IDToken) {
                this.idToken = idToken
                provider = Google
                nonce = rawNonce
            }

            val userId = SupabaseClientProvider.currentUserId()
                ?: return SignInResult.Failure("Signed in, but no user id was returned.")

            SignInResult.Success(userId)

        } catch (e: Exception) {
            SignInResult.Failure("Supabase sign-in failed: ${e.message}")
        }
    }

    suspend fun signOut() {
        SupabaseClientProvider.client.auth.signOut()
    }

    private fun generateRawNonce(): String {
        return UUID.randomUUID().toString()
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
