package kallos.engine

import kotlin.math.round

/**
 * Computes the amount of KP (Kallior Points) the player earns from the
 * difference between the user's radar area and the shadow's radar area.
 */
object CurrencyEngine {

    private const val KP_PER_AREA_UNIT = 5.0

    /**
     * Returns `0` when the user is behind the shadow. Otherwise returns the
     * rounded value of `(userArea - shadowArea) * KP_PER_AREA_UNIT`.
     */
    fun computeKp(userArea: Double, shadowArea: Double): Int {
        if (userArea < shadowArea) return 0
        return round((userArea - shadowArea) * KP_PER_AREA_UNIT).toInt()
    }
}
