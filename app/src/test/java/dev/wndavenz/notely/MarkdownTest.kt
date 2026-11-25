package dev.wndavenz.notely

import dev.wndavenz.notely.utils.Markdown
import dev.wndavenz.notely.utils.MarkdownInline
import dev.wndavenz.notely.utils.MdBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTest {

    @Test
    fun `judul dan paragraf diurai sebagai blok terpisah`() {
        val blocks = Markdown.parse("# Halo\n\nIsi paragraf\nbaris kedua.")
        assertEquals(MdBlock.Heading(1, "Halo"), blocks[0])
        assertEquals(MdBlock.Paragraph("Isi paragraf baris kedua."), blocks[1])
    }

    @Test
    fun `daftar poin tugas dan bernomor dikenali`() {
        val blocks = Markdown.parse("- satu\n- [x] selesai\n- [ ] belum\n\n1. pertama")
        assertEquals(MdBlock.Bullet("satu", null), blocks[0])
        assertEquals(MdBlock.Bullet("selesai", true), blocks[1])
        assertEquals(MdBlock.Bullet("belum", false), blocks[2])
        assertEquals(MdBlock.Numbered("1", "pertama"), blocks[3])
    }

    @Test
    fun `blok kode berpagar utuh termasuk baris mirip judul`() {
        val blocks = Markdown.parse("```kotlin\n# bukan judul\nval x = 1\n```")
        assertEquals(listOf(MdBlock.Code("# bukan judul\nval x = 1", "kotlin")), blocks)
    }

    @Test
    fun `kutipan dan garis pemisah dikenali`() {
        val blocks = Markdown.parse("> kata orang\n> lanjutan\n\n---")
        assertEquals(MdBlock.Quote("kata orang lanjutan"), blocks[0])
        assertEquals(MdBlock.Divider, blocks[1])
    }

    @Test
    fun `gaya sebaris tebal miring kode dan tautan diurai`() {
        val spans = MarkdownInline.parse("biasa **tebal** dan `kode` dan [label](https://x.y)")
        assertTrue(spans.any { it.text == "tebal" && it.bold })
        assertTrue(spans.any { it.text == "kode" && it.code })
        assertTrue(spans.any { it.text == "label" && !it.bold && !it.code })
        assertTrue(spans.none { it.text.contains("https://") })
    }

    @Test
    fun `penanda sebaris tanpa penutup dibiarkan apa adanya`() {
        val spans = MarkdownInline.parse("bintang * tunggal")
        assertEquals("bintang * tunggal", spans.joinToString("") { it.text })
    }
}
