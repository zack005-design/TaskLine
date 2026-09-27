package com.example.taskfoundation.calendar

import java.time.*
import org.junit.Assert.*
import org.junit.Test

class IcsReaderTest {
    private val from = Instant.parse("2026-09-01T00:00:00Z").toEpochMilli()
    private val until = Instant.parse("2026-10-01T00:00:00Z").toEpochMilli()
    private fun read(event: String) = IcsReader.read("BEGIN:VCALENDAR\n$event\nEND:VCALENDAR", from, until, ZoneOffset.UTC)

    @Test fun importsTimezoneFoldedTextAndEscapes() {
        val result = read("""
            BEGIN:VEVENT
            UID:one
            DTSTART;TZID=Asia/Kolkata:20260927T093000
            DTEND;TZID=Asia/Kolkata:20260927T103000
            SUMMARY:Design
             review
            DESCRIPTION:First\nSecond\, third
            END:VEVENT
        """.trimIndent())
        assertEquals("Designreview", result.events.single().title)
        assertEquals("First\nSecond, third", result.events.single().description)
        assertEquals(Instant.parse("2026-09-27T04:00:00Z").toEpochMilli(), result.events.single().start)
        assertEquals(0, result.skipped)
    }

    @Test fun expandsWeeklyRuleAndAppliesExclusions() {
        val result = read("""
            BEGIN:VEVENT
            UID:weekly
            DTSTART:20260901T090000Z
            RRULE:FREQ=WEEKLY;BYDAY=TU,TH;COUNT=5
            EXDATE:20260903T090000Z
            SUMMARY:Practice
            END:VEVENT
        """.trimIndent())
        assertEquals(listOf(1, 8, 10, 15), result.events.map { Instant.ofEpochMilli(it.start).atZone(ZoneOffset.UTC).dayOfMonth })
    }

    @Test fun allDayDateDoesNotShiftInOtherZones() {
        val event = read("BEGIN:VEVENT\nUID:holiday\nDTSTART;VALUE=DATE:20260927\nSUMMARY:Holiday\nEND:VEVENT").events.single()
        val task = event.toTask(0, null, ZoneId.of("America/Los_Angeles"))
        assertEquals(LocalDate.of(2026, 9, 27), com.example.taskfoundation.domain.time.CalendarDates.decode(task.dueDateTime!!))
        assertNull(task.dueTimeMinutes)
    }

    @Test fun unsupportedRulesAreReportedAndCancelledEventsExcluded() {
        val result = read("""
            BEGIN:VEVENT
            UID:unsupported
            DTSTART:20260901T090000Z
            RRULE:FREQ=MONTHLY;BYDAY=1MO
            END:VEVENT
            BEGIN:VEVENT
            UID:cancelled
            DTSTART:20260927T090000Z
            STATUS:CANCELLED
            END:VEVENT
        """.trimIndent())
        assertTrue(result.events.isEmpty())
        assertEquals(1, result.skipped)
    }

    @Test fun monthlyCalendarRulesSkipInvalidDates() {
        val result = IcsReader.read("BEGIN:VCALENDAR\nBEGIN:VEVENT\nUID:m\nDTSTART:20260131T090000Z\nRRULE:FREQ=MONTHLY;COUNT=3\nEND:VEVENT\nEND:VCALENDAR",
            Instant.parse("2026-01-01T00:00:00Z").toEpochMilli(), Instant.parse("2026-06-01T00:00:00Z").toEpochMilli())
        assertEquals(listOf(1, 3, 5), result.events.map { Instant.ofEpochMilli(it.start).atZone(ZoneOffset.UTC).monthValue })
    }
}
