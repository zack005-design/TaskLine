package com.example.taskfoundation.calendar

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Bounded, offline iCalendar reader. Unsupported recurrence sets are reported, never approximated. */
object IcsReader {
    private data class Property(val name: String, val params: Map<String, String>, val value: String)
    private data class DateValue(val time: ZonedDateTime, val allDay: Boolean)

    fun read(text: String, from: Long, until: Long, zone: ZoneId = ZoneId.systemDefault()): CalendarPreview {
        require(text.length <= 10_000_000) { "Calendar file is too large (maximum 10 MB)" }
        require(from < until && until - from <= 367L * 86400000) { "Choose an import range of at most one year" }
        val lines = text.removePrefix("\uFEFF").replace("\r\n", "\n").replace("\r", "\n")
            .replace(Regex("\n[ \t]"), "").lines()
        require(lines.any { it.trim() == "BEGIN:VCALENDAR" } && lines.any { it.trim() == "END:VCALENDAR" }) {
            "Choose an iCalendar (.ics) file. Extract Google Calendar's ZIP export first."
        }
        val blocks = mutableListOf<List<Property>>()
        var current: MutableList<Property>? = null
        var nested = 0
        for (line in lines) {
            when {
                line == "BEGIN:VEVENT" -> { require(current == null) { "Invalid calendar event" }; current = mutableListOf(); nested = 0 }
                line == "END:VEVENT" -> { current?.let { blocks += it }; current = null }
                current != null && line.startsWith("BEGIN:") -> nested++
                current != null && line.startsWith("END:") -> nested--
                current != null && nested == 0 -> {
                    val colon = line.indexOf(':')
                    if (colon > 0) {
                        val parts = line.substring(0, colon).split(';')
                        val params = parts.drop(1).mapNotNull { part ->
                            val split = part.indexOf('='); if (split < 0) null else part.substring(0, split).uppercase() to part.substring(split + 1).trim('"')
                        }.toMap()
                        current.add(Property(parts.first().uppercase(), params, line.substring(colon + 1)))
                    }
                }
            }
        }
        require(current == null) { "Incomplete calendar file" }
        require(blocks.size <= 10000) { "Too many calendar events" }
        // Modified occurrences need a full recurrence-set engine. Device calendar import expands them correctly.
        val overridden = blocks.filter { b -> b.any { it.name == "RECURRENCE-ID" } }
            .mapNotNull { b -> b.find { it.name == "UID" }?.value }.toSet()
        var skipped = 0
        val result = mutableListOf<CalendarEvent>()
        for (block in blocks) {
            try {
                fun prop(name: String) = block.find { it.name == name }
                val uid = requireNotNull(prop("UID")) { "Missing event identifier" }.value
                require(uid !in overridden) { "Modified recurring event" }
                if (prop("STATUS")?.value == "CANCELLED") continue
                require(prop("RDATE") == null && prop("DURATION") == null) { "Unsupported event dates" }
                val start = date(requireNotNull(prop("DTSTART")), zone)
                val end = prop("DTEND")?.let { date(it, zone) } ?: DateValue(
                    if (start.allDay) start.time.plusDays(1) else start.time, start.allDay)
                require(start.allDay == end.allDay && !end.time.isBefore(start.time)) { "Invalid event end" }
                val excluded = block.filter { it.name == "EXDATE" }.flatMap { p -> p.value.split(',').map { date(p.copy(value = it), zone).time.toInstant().toEpochMilli() } }.toSet()
                val rule = prop("RRULE")?.value
                val starts = if (rule == null) listOf(start.time) else expand(start, rule, until, zone)
                val duration = Duration.between(start.time, end.time)
                for (occurrence in starts) {
                    val epoch = occurrence.toInstant().toEpochMilli()
                    if (epoch !in from until until || epoch in excluded) continue
                    require(result.size < 5000) { "Import limit exceeded" }
                    result += CalendarEvent("ics:$uid:$epoch", unescape(prop("SUMMARY")?.value ?: "Untitled event").ifBlank { "Untitled event" },
                        listOfNotNull(prop("DESCRIPTION")?.value?.let(::unescape), prop("LOCATION")?.value?.let { "Location: ${unescape(it)}" }).joinToString("\n"),
                        epoch, occurrence.plus(duration).toInstant().toEpochMilli(), start.allDay)
                }
            } catch (_: IllegalArgumentException) { skipped++ }
              catch (_: DateTimeException) { skipped++ }
        }
        require(result.size < 5000) { "Too many events. Choose a shorter import range." }
        return CalendarPreview(result.distinctBy { it.key }.sortedBy { it.start }, skipped)
    }

    private fun date(p: Property, zone: ZoneId): DateValue {
        val allDay = p.params["VALUE"] == "DATE" || p.value.length == 8
        if (allDay) return DateValue(LocalDate.parse(p.value, DateTimeFormatter.BASIC_ISO_DATE).atStartOfDay(ZoneOffset.UTC), true)
        val utc = p.value.endsWith('Z')
        val tz = if (utc) ZoneOffset.UTC else p.params["TZID"]?.let(ZoneId::of) ?: zone
        val value = p.value.removeSuffix("Z")
        val format = DateTimeFormatter.ofPattern(if (value.length == 13) "uuuuMMdd'T'HHmm" else "uuuuMMdd'T'HHmmss")
            .withResolverStyle(java.time.format.ResolverStyle.STRICT)
        return DateValue(LocalDateTime.parse(value, format).atZone(tz), false)
    }

    private fun expand(start: DateValue, raw: String, until: Long, zone: ZoneId): List<ZonedDateTime> {
        val rule = raw.split(';').associate { val p = it.split('=', limit = 2); require(p.size == 2); p[0] to p[1] }
        require(rule.keys.all { it in setOf("FREQ", "INTERVAL", "COUNT", "UNTIL", "BYDAY", "WKST") })
        val frequency = rule["FREQ"]
        require(frequency in setOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY"))
        val interval = rule["INTERVAL"]?.toInt() ?: 1
        val count = rule["COUNT"]?.toInt() ?: Int.MAX_VALUE
        require(interval in 1..365 && count > 0 && !(rule.containsKey("COUNT") && rule.containsKey("UNTIL")))
        require(rule["WKST"] == null || rule["WKST"] == "MO")
        val byDay = rule["BYDAY"]?.split(',')
        val weekdays = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")
        require(byDay == null || (frequency == "WEEKLY" && byDay.all { it in weekdays }))
        val limit = rule["UNTIL"]?.let { date(Property("UNTIL", emptyMap(), it), start.time.zone).time.toInstant().toEpochMilli() } ?: Long.MAX_VALUE
        val result = mutableListOf<ZonedDateTime>()
        var date = start.time.toLocalDate()
        val anchor = date
        val last = Instant.ofEpochMilli(minOf(until, limit)).atZone(start.time.zone).toLocalDate()
        require(ChronoUnit.DAYS.between(anchor, last) <= 366L * 200) { "Recurrence history too long" }
        var generated = 0
        while (!date.isAfter(last) && generated < count) {
            val matches = when (frequency) {
                "DAILY" -> ChronoUnit.DAYS.between(anchor, date) % interval == 0L
                "WEEKLY" -> {
                    val a = anchor.minusDays((anchor.dayOfWeek.value - 1).toLong())
                    val b = date.minusDays((date.dayOfWeek.value - 1).toLong())
                    ChronoUnit.WEEKS.between(a, b) % interval == 0L &&
                        (byDay?.contains(weekdays[date.dayOfWeek.value - 1]) ?: (date.dayOfWeek == anchor.dayOfWeek))
                }
                "MONTHLY" -> ChronoUnit.MONTHS.between(YearMonth.from(anchor), YearMonth.from(date)) % interval == 0L && date.dayOfMonth == anchor.dayOfMonth
                else -> (date.year - anchor.year) % interval == 0 && date.month == anchor.month && date.dayOfMonth == anchor.dayOfMonth
            }
            if (matches || date == anchor) {
                val occurrence = date.atTime(start.time.toLocalTime()).atZone(start.time.zone)
                if (occurrence.toInstant().toEpochMilli() > limit) break
                generated++; result += occurrence
            }
            date = date.plusDays(1)
        }
        return result
    }

    private fun unescape(value: String): String = Regex("\\\\([nN,;\\\\])").replace(value) {
        when (it.groupValues[1]) { "n", "N" -> "\n"; else -> it.groupValues[1] }
    }
}
