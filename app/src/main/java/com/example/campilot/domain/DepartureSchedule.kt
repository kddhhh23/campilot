package com.example.campilot.domain

import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime

/** Local tomorrow, including month/year rollover. Planning period includes outbound travel. */
data class DepartureSchedule(val now: ZonedDateTime, val departure: ZonedDateTime) {
    val hours: Double get() = Duration.between(now, departure).let {
        (it.seconds + it.nano / 1_000_000_000.0) / 3600.0
    }

    companion object {
        fun from(now: ZonedDateTime, minuteOfDay: Int): DepartureSchedule {
            require(minuteOfDay in 0 until 24 * 60)
            val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
            return DepartureSchedule(now, now.toLocalDate().plusDays(1).atTime(time).atZone(now.zone))
        }
    }
}
