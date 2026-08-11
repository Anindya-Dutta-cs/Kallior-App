package kallos.platform

class SpotifyAlarm {
    fun setPlaylistUri(uri: String) = Unit
    fun getPlaylistUri(): String? = null
    suspend fun playOnDismiss() = Unit
}
