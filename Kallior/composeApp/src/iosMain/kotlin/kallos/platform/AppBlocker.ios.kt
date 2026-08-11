package kallos.platform

class AppBlocker {
    fun isBlockingEnabled(): Boolean = false
    fun setBlockingEnabled(enabled: Boolean) = Unit
    fun isPremiumUnlocked(): Boolean = false
    suspend fun unlockViaPayment(): Boolean = false
}
