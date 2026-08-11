package kallos.platform

fun calculateIosDisciplineScore(minutes: Int): Int {
    val excess = maxOf(0, minutes - 90)
    return maxOf(0, 100 - ((excess / 30.0) * 4).toInt())
}
