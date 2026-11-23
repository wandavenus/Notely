package dev.wndavenz.notely

import dev.wndavenz.notely.update.AppUpdater
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Uji perbandingan versi untuk pemeriksa pembaruan. */
class VersionCompareTest {

    @Test
    fun `versi lebih besar dianggap lebih baru`() {
        assertTrue(AppUpdater.isNewer("1.1.0", "1.0.1"))
        assertTrue(AppUpdater.isNewer("1.0.2", "1.0.1"))
        assertTrue(AppUpdater.isNewer("2.0.0", "1.9.9"))
    }

    @Test
    fun `versi sama atau lebih kecil bukan pembaruan`() {
        assertFalse(AppUpdater.isNewer("1.0.1", "1.0.1"))
        assertFalse(AppUpdater.isNewer("1.0.0", "1.0.1"))
        assertFalse(AppUpdater.isNewer("0.9.9", "1.0.0"))
    }

    @Test
    fun `awalan v dan ruas hilang ditoleransi`() {
        assertTrue(AppUpdater.isNewer("v1.2", "1.1.9"))
        assertFalse(AppUpdater.isNewer("1.2", "1.2.0"))
    }
}
