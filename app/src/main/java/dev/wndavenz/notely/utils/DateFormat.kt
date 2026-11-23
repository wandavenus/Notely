package dev.wndavenz.notely.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Format tanggal/waktu Bahasa Indonesia mengikuti gaya Apple Notes. */
object DateFormat {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val localeId = Locale.forLanguageTag("id-ID")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH.mm", localeId)
    private val fullFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy  HH.mm", localeId)

    /** Contoh: 17.50 */
    fun time(epochMillis: Long): String {
        return Instant.ofEpochMilli(epochMillis).atZone(zone).format(timeFormatter)
    }

    /** Contoh: 24 November 2026  01.05 (baris tanggal di editor). */
    fun fullDateTime(epochMillis: Long): String {
        return Instant.ofEpochMilli(epochMillis).atZone(zone).format(fullFormatter)
    }
}

/** Kelompok tanggal untuk seksi daftar catatan. */
enum class NoteDateGroup(val label: String) {
    TODAY("Hari Ini"),
    YESTERDAY("Kemarin"),
    LAST_7_DAYS("7 Hari Terakhir"),
    LAST_30_DAYS("30 Hari Terakhir"),
    OLDER("Lebih Lama"),
}

fun dateGroupOf(epochMillis: Long, now: LocalDate = LocalDate.now()): NoteDateGroup {
    val day = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val diffDays = now.toEpochDay() - day.toEpochDay()
    return when {
        diffDays <= 0 -> NoteDateGroup.TODAY
        diffDays == 1L -> NoteDateGroup.YESTERDAY
        diffDays <= 7L -> NoteDateGroup.LAST_7_DAYS
        diffDays <= 30L -> NoteDateGroup.LAST_30_DAYS
        else -> NoteDateGroup.OLDER
    }
}
