package dev.wndavenz.notely

import dev.wndavenz.notely.utils.CodeHighlight
import dev.wndavenz.notely.utils.CodeTokenType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeHighlightTest {

    private fun typesOf(code: String, language: String = "") =
        CodeHighlight.highlight(code, language).associate { it.text to it.type }

    @Test
    fun `potongan digabung sama dengan kode asal`() {
        val code = "val nama = \"Memi\" // halo\nfun main() { print(42) }"
        val joined = CodeHighlight.highlight(code, "kotlin").joinToString("") { it.text }
        assertEquals(code, joined)
    }

    @Test
    fun `kata kunci string angka dan komentar diwarnai`() {
        val types = typesOf("val x = \"teks\" // catatan\nval y = 42", "kotlin")
        assertEquals(CodeTokenType.KEYWORD, types["val"])
        assertEquals(CodeTokenType.STRING, types["\"teks\""])
        assertEquals(CodeTokenType.COMMENT, types["// catatan"])
        assertEquals(CodeTokenType.NUMBER, types["42"])
    }

    @Test
    fun `isi string tidak ikut diurai sebagai kata kunci`() {
        val spans = CodeHighlight.highlight("val s = \"return false\"", "kotlin")
        val stringSpan = spans.single { it.type == CodeTokenType.STRING }
        assertEquals("\"return false\"", stringSpan.text)
    }

    @Test
    fun `nama fungsi dan tipe dikenali`() {
        val types = typesOf("val catatan = Catatan(buatBaru())")
        assertEquals(CodeTokenType.TYPE, types["Catatan"])
        assertEquals(CodeTokenType.FUNCTION, types["buatBaru"])
    }

    @Test
    fun `komentar pagar hanya untuk bahasa skrip`() {
        val python = typesOf("# komentar\nx = 1", "python")
        assertEquals(CodeTokenType.COMMENT, python["# komentar"])
        val kotlin = CodeHighlight.highlight("#judul", "kotlin")
        assertTrue(kotlin.none { it.type == CodeTokenType.COMMENT })
    }

    @Test
    fun `anotasi dikenali`() {
        val types = typesOf("@Composable fun Layar() {}")
        assertEquals(CodeTokenType.ANNOTATION, types["@Composable"])
    }
}
