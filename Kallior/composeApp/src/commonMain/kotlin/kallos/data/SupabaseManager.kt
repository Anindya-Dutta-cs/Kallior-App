package kallos.data

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Public client configuration. The publishable/anon key is safe to ship only with RLS enabled. */
data class SupabaseConfig(
    val url: String,
    val publishableKey: String,
)

sealed interface AuthenticationState {
    data object Loading : AuthenticationState
    data object Unauthenticated : AuthenticationState
    data class Authenticated(val userId: String) : AuthenticationState
    data class ConfigurationError(val message: String) : AuthenticationState
}

/**
 * Owns the single public-key Supabase client. Platform entry points must call [configure]
 * before accessing [client]; this keeps deployment values out of common source and Git.
 */
object SupabaseManager {
    private var config: SupabaseConfig? = null

    fun configure(config: SupabaseConfig) {
        require(config.url.startsWith("https://")) { "Supabase URL must use HTTPS." }
        require(config.publishableKey.isNotBlank()) { "Supabase publishable key is required." }
        this.config = config
    }

    val isConfigured: Boolean get() = config != null

    val client by lazy {
        val currentConfig = requireNotNull(config) {
            "SupabaseManager.configure must be called before using Supabase."
        }
        createSupabaseClient(
            supabaseUrl = currentConfig.url,
            supabaseKey = currentConfig.publishableKey,
        ) {
            install(Auth) {
                flowType = FlowType.PKCE
                scheme = "kallior"
                host = "auth"
            }
            install(Postgrest)
        }
    }

    fun authenticationStates(): Flow<AuthenticationState> {
        if (!isConfigured) {
            return kotlinx.coroutines.flow.flowOf(
                AuthenticationState.ConfigurationError("Supabase is not configured for this build."),
            )
        }
        return client.auth.sessionStatus.map { status ->
            when (status) {
                SessionStatus.Initializing -> AuthenticationState.Loading
                is SessionStatus.Authenticated -> AuthenticationState.Authenticated(status.session.user?.id ?: "")
                is SessionStatus.NotAuthenticated,
                is SessionStatus.RefreshFailure -> AuthenticationState.Unauthenticated
            }
        }
    }

    fun requireCurrentUserId(): String = requireNotNull(client.auth.currentUserOrNull()?.id) {
        "An authenticated user is required to access user-owned data."
    }
}
