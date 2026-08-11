package kallos.engine

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Injectable clock for testable time-based rules. */
interface GameClock {
    fun now(): Instant
    fun today(): LocalDate =
        now().toLocalDateTime(TimeZone.currentSystemDefault()).date
}

class SystemGameClock : GameClock {
    override fun now(): Instant = Clock.System.now()
}
