package com.example.bananaq

import com.example.bananaq.data.ScanDateGrouping
import com.example.bananaq.data.ScanDateGrouping.Period
import com.example.bananaq.data.ScanRecord
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ScanDateGroupingTest {
    private val manila = TimeZone.getTimeZone("Asia/Manila")

    private fun timestamp(value: String, zone: TimeZone = manila): Long =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
            timeZone = zone
        }.parse(value)!!.time

    private fun scan(id: String, value: String, zone: TimeZone = manila) =
        ScanRecord(id, "session", null, "Healthy", .9, true, timestamp(value, zone))

    @Test fun sectionsAreExclusiveAndNewestFirst() {
        val records = listOf(
            scan("older", "2026-09-20 12:00"),
            scan("todayEarly", "2026-10-03 00:00"),
            scan("lastWeek", "2026-09-27 23:59"),
            scan("thisWeek", "2026-09-28 00:00"),
            scan("yesterday", "2026-10-02 23:59"),
            scan("todayLate", "2026-10-03 10:00")
        )
        val groups = ScanDateGrouping.group(records, timestamp("2026-10-03 12:00"), manila)
        assertEquals(Period.entries.toList(), groups.keys.toList())
        assertEquals(listOf("todayLate", "todayEarly"), groups[Period.TODAY]!!.map { it.scanId })
        assertEquals(listOf("thisWeek"), groups[Period.THIS_WEEK]!!.map { it.scanId })
        assertEquals(records.size, groups.values.sumOf { it.size })
    }

    @Test fun mondayKeepsYesterdaySeparateFromLastWeek() {
        val groups = ScanDateGrouping.group(listOf(
            scan("sunday", "2026-10-04 12:00"),
            scan("saturday", "2026-10-03 12:00"),
            scan("previousMonday", "2026-09-28 00:00"),
            scan("olderSunday", "2026-09-27 23:59")
        ), timestamp("2026-10-05 12:00"), manila)
        assertEquals(listOf(Period.YESTERDAY, Period.LAST_WEEK, Period.OLDER), groups.keys.toList())
        assertEquals(2, groups[Period.LAST_WEEK]!!.size)
    }

    @Test fun yearBoundaryUsesCalendarDates() {
        val groups = ScanDateGrouping.group(listOf(
            scan("yesterday", "2025-12-31 23:59"),
            scan("thisWeek", "2025-12-29 00:00"),
            scan("lastWeek", "2025-12-22 00:00")
        ), timestamp("2026-01-01 01:00"), manila)
        assertEquals(listOf(Period.YESTERDAY, Period.THIS_WEEK, Period.LAST_WEEK), groups.keys.toList())
    }

    @Test fun localMidnightDoesNotUseUtcDay() {
        val record = ScanRecord("localYesterday", "session", null, null, null, false,
            timestamp("2026-10-02 15:59", TimeZone.getTimeZone("UTC")))
        val groups = ScanDateGrouping.group(listOf(record), timestamp("2026-10-03 00:01"), manila)
        assertEquals(listOf(Period.YESTERDAY), groups.keys.toList())
    }

    @Test fun daylightSavingDaysUseCalendarSubtraction() {
        val zone = TimeZone.getTimeZone("America/New_York")
        val groups = ScanDateGrouping.group(listOf(
            scan("yesterday", "2026-03-08 00:30", zone)
        ), timestamp("2026-03-09 00:15", zone), zone)
        assertEquals(listOf(Period.YESTERDAY), groups.keys.toList())
    }

    @Test fun noScansProduceNoEmptySections() {
        assertTrue(ScanDateGrouping.group(emptyList(), timestamp("2026-10-03 12:00"), manila).isEmpty())
    }
}
