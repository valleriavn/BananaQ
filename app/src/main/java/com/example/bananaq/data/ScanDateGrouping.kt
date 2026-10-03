package com.example.bananaq.data

import java.util.Calendar
import java.util.TimeZone

/** Calendar-based sections, using the device timezone and Monday as the start of a week. */
object ScanDateGrouping {
    enum class Period { TODAY, YESTERDAY, THIS_WEEK, LAST_WEEK, OLDER }

    fun group(
        scans: List<ScanRecord>,
        now: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Map<Period, List<ScanRecord>> {
        val calendar = Calendar.getInstance(timeZone).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val today = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = calendar.timeInMillis
        calendar.timeInMillis = today
        val daysSinceMonday = (calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        calendar.add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
        val thisWeek = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val lastWeek = calendar.timeInMillis

        // Newest first within each section; groupBy preserves that section order.
        return scans.sortedByDescending { it.scannedAt }.groupBy { scan ->
            when {
                scan.scannedAt >= today -> Period.TODAY
                scan.scannedAt >= yesterday -> Period.YESTERDAY
                scan.scannedAt >= thisWeek -> Period.THIS_WEEK
                scan.scannedAt >= lastWeek -> Period.LAST_WEEK
                else -> Period.OLDER
            }
        }
    }
}
