package dev.wndavenz.notely

import dev.wndavenz.notely.utils.NoteText
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteTextTest {

    @Test
    fun `judul adalah baris non-kosong pertama`() {
        assertEquals("Belanja", NoteText.titleOf("\n\n  Belanja  \nSusu\nTelur"))
    }

    @Test
    fun `judul catatan kosong memakai teks bawaan`() {
        assertEquals(NoteText.EMPTY_TITLE, NoteText.titleOf(""))
        assertEquals(NoteText.EMPTY_TITLE, NoteText.titleOf("   \n \n"))
    }

    @Test
    fun `pratinjau adalah baris setelah judul`() {
        assertEquals("Susu dan telur", NoteText.previewOf("Belanja\n\nSusu dan telur\nRoti"))
    }

    @Test
    fun `pratinjau tanpa baris lanjutan memakai teks bawaan`() {
        assertEquals(NoteText.EMPTY_PREVIEW, NoteText.previewOf("Hanya judul"))
        assertEquals(NoteText.EMPTY_PREVIEW, NoteText.previewOf(""))
    }
}
