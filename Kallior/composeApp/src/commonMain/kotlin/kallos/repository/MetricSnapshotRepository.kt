package kallos.repository

import kallos.domain.DailyMetricSnapshot
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

class MetricSnapshotRepository {
    private val _snapshots = mutableListOf<DailyMetricSnapshot>()
    val snapshots: List<DailyMetricSnapshot> get() = _snapshots.toList()

    fun addOrUpdate(snapshot: DailyMetricSnapshot) {
        val index = _snapshots.indexOfFirst { it.date == snapshot.date }
        if (index != -1) _snapshots[index] = snapshot
        else _snapshots.add(snapshot)
    }

    fun replaceAll(snapshots: List<DailyMetricSnapshot>) {
        _snapshots.clear()
        _snapshots.addAll(snapshots)
    }

    fun window(today: LocalDate, days: Int = 4): List<DailyMetricSnapshot> {
        val cutoff = today.minus(DatePeriod(days = days - 1))
        return _snapshots.filter { it.date >= cutoff }.sortedBy { it.date }
    }
}
