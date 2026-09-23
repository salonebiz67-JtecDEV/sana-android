# Sana — Google Sign-In ("Continue with Google")

No email/password screen. One tap, using the account already on
the phone.

## What you actually need to do (mostly console setup, not code)

### 1. Google Cloud Console

1. Go to console.cloud.google.com -> APIs & Services -> Credentials
2. Create **two** OAuth 2.0 Client IDs:
   - **Web application** — you'll paste this one into Supabase, and
     it's also the `webClientId` this code needs
   - **Android** — needs your app's package name (e.g.
     `com.johnfatoma.sana`) and its SHA-1 signing certificate
     fingerprint (get this by running `./gradlew signingReport` in
     Android Studio's terminal, for your debug and later release
     keystores)

Both must exist for Sign in with Google to work on Android, even
though only the Web Client ID appears directly in this code.

### 2. Supabase Dashboard

1. Authentication -> Providers -> Google
2. Paste in the **Web** Client ID and its client secret
3. Enable the provider

### 3. Fill in the placeholders in this code

- `SupabaseClientProvider.kt` — `SUPABASE_URL` and `SUPABASE_ANON_KEY`
  (the **anon/public** key, Project Settings -> API — never the
  service_role key, that one stays backend-only)
- Wherever you instantiate `GoogleSignInManager` — pass your Web
  Client ID as `webClientId`

## Required Gradle dependencies

```kotlin
dependencies {
    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    implementation(platform("io.github.jan-tennert.supabase:bom:3.1.0"))
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.ktor:ktor-client-android:3.0.0")
}
```

(Check for newer versions before building — this stack moves fast.)

## Usage

```kotlin
val signInManager = GoogleSignInManager(
    context = this,
    webClientId = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com",
)

lifecycleScope.launch {
    when (val result = signInManager.signIn()) {
        is GoogleSignInManager.SignInResult.Success -> {
            // result.userId is the Supabase user id.
            // SupabaseClientProvider.currentAccessToken() now returns
            // a real token — use it exactly like before for /chat,
            // /actions/result, /live/ws, etc.
            val accessToken = SupabaseClientProvider.currentAccessToken()
        }
        is GoogleSignInManager.SignInResult.Failure -> {
            showError(result.message)
        }
        GoogleSignInManager.SignInResult.Cancelled -> {
            // User backed out of the account picker — not an error,
            // don't show a scary message for this one.
        }
    }
}
```

## Why this needs no backend changes

`get_current_user` in `app/auth/dependencies.py` verifies Supabase
access tokens — it has no idea whether the user signed in with
email or Google, and it doesn't need to. Supabase issues the same
shape of token either way. Google Sign-In is entirely a client-side
concern.

## Honest gaps

- **Release builds need their own SHA-1.** The Android OAuth Client
  ID you create in Google Cloud Console is tied to a specific
  signing certificate. Your debug builds and your eventual Play
  Store release build have *different* SHA-1 fingerprints — you'll
  need to add the release one to the same Android Client ID before
  Google Sign-In works in a signed release build, or it will fail
  silently with a generic credential error.
- **No account-linking UI.** If a user signs up with Google, then
  later somehow ends up trying email/password with the same
  address, Supabase's default behavior (link by matching email) may
  or may not be what you want — worth testing deliberately once
  there's more than one auth method live.
