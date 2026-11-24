package dev.wndavenz.notely

import dev.wndavenz.notely.vault.VaultStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Uji hash PIN brankas (fungsi murni di [VaultStore]). */
class VaultStoreTest {

    private val salt = ByteArray(16) { it.toByte() }
    private val otherSalt = ByteArray(16) { (it + 100).toByte() }

    @Test
    fun `hash deterministik untuk pin dan garam yang sama`() {
        val first = VaultStore.hashPin("1234", salt)
        val second = VaultStore.hashPin("1234", salt)
        assertTrue(first.contentEquals(second))
        assertEquals(32, first.size)
    }

    @Test
    fun `hash berbeda untuk pin berbeda`() {
        assertFalse(
            VaultStore.hashPin("1234", salt)
                .contentEquals(VaultStore.hashPin("1235", salt)),
        )
    }

    @Test
    fun `hash berbeda untuk garam berbeda`() {
        assertFalse(
            VaultStore.hashPin("1234", salt)
                .contentEquals(VaultStore.hashPin("1234", otherSalt)),
        )
    }
}
