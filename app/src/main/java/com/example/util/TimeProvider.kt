package com.example.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

interface TimeProvider {
    val zoneId: ZoneId
    fun now(): ZonedDateTime
    fun today(): LocalDate = now().toLocalDate()
    fun currentYearMonth(): YearMonth = YearMonth.from(today())
    fun nowEpochMs(): Long = now().toInstant().toEpochMilli()
    fun atBangladeshDateTime(date: LocalDate, time: LocalTime): ZonedDateTime =
        ZonedDateTime.of(date, time, zoneId)
    fun fromEpochMs(epochMs: Long): ZonedDateTime =
        Instant.ofEpochMilli(epochMs).atZone(zoneId)
}

class BangladeshTimeProvider : TimeProvider {
    override val zoneId: ZoneId = ZoneId.of("Asia/Dhaka")
    override fun now(): ZonedDateTime = ZonedDateTime.now(zoneId)
}
