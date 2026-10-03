Fix the Supabase OAuth deep-link flow in this Kotlin Multiplatform Android app (Kallior). 
Google sign-in completes on the Google/Supabase side but the app never receives control back 
— it hangs on the browser/Supabase screen instead of returning to the app.

Context:
- Supabase project: pxwssxfeissjhbhvdtzl.supabase.co (also referenced elsewhere as hbhvdtzl — 
  confirm which is the actual active project ref and use that consistently)
- Intended deep link scheme: kallior://auth-callback
- Android app module is `androidApp` (composeApp is a library, not the runnable app)

Please do the following:

1. Search the codebase for the current Supabase client setup (likely using supabase-kt / 
   `io.github.jan-tennert.supabase`) and identify which auth library and version is in use.

2. Find where `signInWithOAuth` (or equivalent) is called for the Google provider. Verify it 
   explicitly passes `redirectTo = "kallior://auth-callback"` (or the correct scheme/host — 
   confirm against AndroidManifest.xml). Fix if missing or mismatched.

3. Check AndroidManifest.xml for an intent-filter on the launcher/main activity that catches 
   this custom scheme, e.g.:
   <intent-filter>
       <action android:name="android.intent.action.VIEW" />
       <category android:name="android.intent.category.DEFAULT" />
       <category android:name="android.intent.category.BROWSABLE" />
       <data android:scheme="kallior" android:host="auth-callback" />
   </intent-filter>
   Add it if missing, on the correct exported activity.

4. Check MainActivity (or wherever the activity lifecycle is handled) for code that captures 
   the incoming deep link Intent (both `onCreate` for cold start and `onNewIntent` for warm 
   start) and hands it to the Supabase client to complete the session — e.g. 
   `supabase.handleDeeplinks(intent)` or the equivalent for whatever auth library version is 
   in use. Add this if it's missing — this is the most likely root cause of the hang.

5. Once the deep link is captured and passed to Supabase, confirm the app then checks/observes 
   the Supabase session state (e.g. `supabase.auth.sessionStatus`) and navigates the user into 
   the app's authenticated flow. Add this transition if it doesn't exist yet.

6. Do NOT touch remote config (Supabase dashboard settings, Google Cloud Console OAuth client) 
   — those have already been fixed. Only fix app-side code.

After changes, summarize exactly what was missing and what you added/changed, file by file.