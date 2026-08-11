package kallos.platform

object DeviceActivityReporter {
    fun reportExactEntertainmentMinutes(minutes: Int) {
        IosScreenTimeState.exactEndOfDayMinutes = minutes
    }
}
