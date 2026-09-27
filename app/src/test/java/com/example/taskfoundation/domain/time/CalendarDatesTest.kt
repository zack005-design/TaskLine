package com.example.taskfoundation.domain.time

import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class CalendarDatesTest {
    @Test fun calendarDateSurvivesDeviceZoneChanges() {
        val original = TimeZone.getDefault()
        try {
            val day = LocalDate.of(2026, 9, 26)
            val value = CalendarDates.encode(day)
            listOf("Asia/Kolkata", "America/Los_Angeles", "Pacific/Kiritimati").forEach {
                TimeZone.setDefault(TimeZone.getTimeZone(it))
                assertEquals(day, CalendarDates.decode(value))
                assertTrue(CalendarDates.isValid(value))
            }
        } finally { TimeZone.setDefault(original) }
    }

    @Test fun legacyDatesPreserveTheirDayAcrossDstBoundaries() {
        listOf("Asia/Kolkata", "America/New_York", "Pacific/Auckland").forEach { name ->
            val zone = ZoneId.of(name)
            listOf(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 11, 1)).forEach { day ->
                val old = day.atStartOfDay(zone).toInstant().toEpochMilli()
                assertEquals(CalendarDates.encode(day), CalendarDates.migrateLegacy(old, zone))
            }
        }
        assertFalse(CalendarDates.isValid(123L))
    }
}
