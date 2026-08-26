package com.avinal.memos

import com.avinal.memos.ui.components.toRelativeString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

class RelativeTimestampTest {

    private val tz = TimeZone.UTC
    private val now = Instant.parse("2026-08-26T12:00:00Z")

    @Test fun justNow0Seconds() = assertEquals("just now", (now - 0.seconds).toRelativeString(now, tz))
    @Test fun justNow30Seconds() = assertEquals("just now", (now - 30.seconds).toRelativeString(now, tz))
    @Test fun justNow59Seconds() = assertEquals("just now", (now - 59.seconds).toRelativeString(now, tz))

    @Test fun oneMinuteAgo() = assertEquals("1m ago", (now - 1.minutes).toRelativeString(now, tz))
    @Test fun thirtyMinutesAgo() = assertEquals("30m ago", (now - 30.minutes).toRelativeString(now, tz))
    @Test fun fiftyNineMinutesAgo() = assertEquals("59m ago", (now - 59.minutes).toRelativeString(now, tz))

    @Test fun oneHourAgo() = assertEquals("1h ago", (now - 1.hours).toRelativeString(now, tz))
    @Test fun twelveHoursAgo() = assertEquals("12h ago", (now - 12.hours).toRelativeString(now, tz))

    @Test fun yesterday() {
        val yesterdayMorning = Instant.parse("2026-08-25T08:00:00Z")
        assertEquals("yesterday", yesterdayMorning.toRelativeString(now, tz))
    }

    @Test fun twoDaysAgoShowsDayName() {
        val twoDaysAgo = Instant.parse("2026-08-24T12:00:00Z")
        assertEquals("Mon", twoDaysAgo.toRelativeString(now, tz))
    }

    @Test fun sixDaysAgoShowsDayName() {
        val sixDaysAgo = Instant.parse("2026-08-20T12:00:00Z")
        assertEquals("Thu", sixDaysAgo.toRelativeString(now, tz))
    }

    @Test fun sevenDaysAgoShowsMonthDay() {
        val sevenDaysAgo = Instant.parse("2026-08-19T12:00:00Z")
        assertEquals("Aug 19", sevenDaysAgo.toRelativeString(now, tz))
    }

    @Test fun sameYearShowsMonthDay() {
        val earlier = Instant.parse("2026-03-15T10:00:00Z")
        assertEquals("Mar 15", earlier.toRelativeString(now, tz))
    }

    @Test fun differentYearShowsFull() {
        val lastYear = Instant.parse("2025-12-25T10:00:00Z")
        assertEquals("Dec 25, 2025", lastYear.toRelativeString(now, tz))
    }

    @Test fun janFirstDifferentYear() {
        val jan1 = Instant.parse("2024-01-01T00:00:00Z")
        assertEquals("Jan 1, 2024", jan1.toRelativeString(now, tz))
    }

    @Test fun midnightBoundaryShowsHours() {
        val justBeforeMidnight = Instant.parse("2026-08-25T23:59:59Z")
        assertEquals("12h ago", justBeforeMidnight.toRelativeString(now, tz))
    }

    @Test fun yesterdayMorningShowsYesterday() {
        val yesterdayEarly = Instant.parse("2026-08-25T06:00:00Z")
        assertEquals("yesterday", yesterdayEarly.toRelativeString(now, tz))
    }

    @Test fun hoursAgoSameDay() {
        val earlyToday = Instant.parse("2026-08-26T02:00:00Z")
        assertEquals("10h ago", earlyToday.toRelativeString(now, tz))
    }

    @Test fun futureTimestampShowsJustNow() {
        val future = now + 5.minutes
        assertEquals("just now", future.toRelativeString(now, tz))
    }
}
