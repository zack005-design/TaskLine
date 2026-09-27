package com.example.taskfoundation.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** Calendar dates use UTC midnight as an encoding, never as a local instant. */
object CalendarDates {
    fun encode(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun decode(value: Long): LocalDate = Instant.ofEpochMilli(value).atZone(ZoneOffset.UTC).toLocalDate()

    // Version 1 did not record the original zone. Preserve the date displayed in the
    // device's zone at migration time; subsequent zone changes cannot shift it.
    fun migrateLegacy(value: Long, zone: ZoneId): Long =
        encode(Instant.ofEpochMilli(value).atZone(zone).toLocalDate())

    fun isValid(value: Long): Boolean =
        value == encode(decode(value)) && decode(value).year in 1..9999
}
