package dev.wndavenz.notely

import dev.wndavenz.notely.utils.NoteDateGroup
import dev.wndavenz.notely.utils.dateGroupOf
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class DateGroupTest {

    private val today: LocalDate = LocalDate.of(2026, 11, 24)

    private fun millis(date: LocalDate): Long {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    @Test
    fun `hari ini dan kemarin`() {
        assertEquals(NoteDateGroup.TODAY, dateGroupOf(millis(today), today))
        assertEquals(NoteDateGroup.YESTERDAY, dateGroupOf(millis(today.minusDays(1)), today))
    }

    @Test
    fun `rentang tujuh dan tiga puluh hari`() {
        assertEquals(NoteDateGroup.LAST_7_DAYS, dateGroupOf(millis(today.minusDays(5)), today))
        assertEquals(NoteDateGroup.LAST_30_DAYS, dateGroupOf(millis(today.minusDays(20)), today))
    }

    @Test
    fun `lebih lama dari tiga puluh hari`() {
        assertEquals(NoteDateGroup.OLDER, dateGroupOf(millis(today.minusDays(45)), today))
    }
}
