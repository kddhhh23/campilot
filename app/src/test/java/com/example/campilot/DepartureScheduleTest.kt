package com.example.campilot

import com.example.campilot.domain.DepartureSchedule
import java.time.ZonedDateTime
import org.junit.Assert.*
import org.junit.Test

class DepartureScheduleTest {
    @Test fun eveningPlansUseTomorrowNotToday() {
        val s = DepartureSchedule.from(ZonedDateTime.parse("2026-11-14T22:00:00+09:00[Asia/Seoul]"), 9 * 60)
        assertEquals("2026-11-15T09:00+09:00[Asia/Seoul]", s.departure.toString())
        assertEquals(11.0, s.hours, .0001)
    }
    @Test fun yearEndAndMidnightStayPositive() {
        val s = DepartureSchedule.from(ZonedDateTime.parse("2026-12-31T23:59:00+09:00[Asia/Seoul]"), 0)
        assertEquals("2027-01-01T00:00+09:00[Asia/Seoul]", s.departure.toString())
        assertEquals(1.0 / 60, s.hours, .0001)
    }
    @Test fun fractionalSecondBeforeMidnightStillHasPositiveBudget() {
        val s = DepartureSchedule.from(ZonedDateTime.parse("2026-12-31T23:59:59.500+09:00[Asia/Seoul]"), 0)
        assertTrue(s.hours > 0)
        assertEquals(0.5 / 3600, s.hours, 1e-10)
    }
    @Test fun newDayMovesTomorrowForward() {
        val before = DepartureSchedule.from(ZonedDateTime.parse("2026-11-30T23:59:00+09:00[Asia/Seoul]"), 10 * 60)
        val after = DepartureSchedule.from(ZonedDateTime.parse("2026-12-01T00:00:00+09:00[Asia/Seoul]"), 10 * 60)
        assertEquals("2026-12-01", before.departure.toLocalDate().toString())
        assertEquals("2026-12-02", after.departure.toLocalDate().toString())
        assertEquals(34.0, after.hours, .0001)
    }
    @Test fun customMinutesArePreserved() {
        val s = DepartureSchedule.from(ZonedDateTime.parse("2026-11-14T22:00:00+09:00[Asia/Seoul]"), 8 * 60 + 45)
        assertEquals("08:45", s.departure.toLocalTime().toString())
        assertEquals(10.75, s.hours, .0001)
    }
    @Test(expected = IllegalArgumentException::class) fun invalidTimeCannotCreatePlan() {
        DepartureSchedule.from(ZonedDateTime.now(), 1440)
    }
}
