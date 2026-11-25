package dev.wndavenz.notely.utils

/** Blok hasil urai Markdown sederhana untuk layar baca dokumen. */
sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class Bullet(val text: String, val checked: Boolean?) : MdBlock
    data class Numbered(val number: String, val text: String) : MdBlock
    data class Code(val text: String, val language: String = "") : MdBlock
    data class Quote(val text: String) : MdBlock
    data object Divider : MdBlock
}

/**
 * Pengurai Markdown yang disengaja kecil: judul, paragraf, daftar, kutipan,
 * blok kode berpagar, dan garis pemisah. Gaya sebaris (tebal/miring/kode)
 * diurai terpisah oleh [MarkdownInline] saat blok ditampilkan.
 */
object Markdown {

    private val headingRegex = Regex("""^(#{1,6})\s+(.*)$""")
    private val numberedRegex = Regex("""^(\d+)[.)]\s+(.*)$""")
    private val taskRegex = Regex("""^\[( |x|X)]\s+(.*)$""")

    fun parse(source: String): List<MdBlock> = parseWithLines(source).map { it.first }

    /** Seperti [parse], tapi tiap blok disertai nomor baris sumbernya (mulai 1). */
    fun parseWithLines(source: String): List<Pair<MdBlock, Int>> {
        val blocks = ArrayList<Pair<MdBlock, Int>>()
        val lines = source.replace("\r\n", "\n").split('\n')
        var index = 0
        while (index < lines.size) {
            val raw = lines[index]
            val line = raw.trim()
            val blockStart = index + 1
            fun emit(block: MdBlock) {
                blocks.add(block to blockStart)
            }
            when {
                line.isEmpty() -> index++

                line.startsWith("```") || line.startsWith("~~~") -> {
                    val fence = line.take(3)
                    val language = line.drop(3).trim().lowercase()
                    val code = StringBuilder()
                    index++
                    while (index < lines.size && !lines[index].trim().startsWith(fence)) {
                        if (code.isNotEmpty()) code.append('\n')
                        code.append(lines[index])
                        index++
                    }
                    index++ // lewati pagar penutup (atau EOF)
                    emit(MdBlock.Code(code.toString(), language))
                }

                headingRegex.matches(line) -> {
                    val match = headingRegex.matchEntire(line)!!
                    emit(
                        MdBlock.Heading(
                            level = match.groupValues[1].length,
                            text = match.groupValues[2].trim(),
                        ),
                    )
                    index++
                }

                line == "---" || line == "***" || line == "___" -> {
                    emit(MdBlock.Divider)
                    index++
                }

                line.startsWith(">") -> {
                    val parts = ArrayList<String>()
                    while (index < lines.size && lines[index].trim().startsWith(">")) {
                        parts.add(lines[index].trim().removePrefix(">").trim())
                        index++
                    }
                    emit(MdBlock.Quote(parts.joinToString(" ")))
                }

                line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ") -> {
                    val content = line.drop(2).trim()
                    val task = taskRegex.matchEntire(content)
                    if (task != null) {
                        emit(
                            MdBlock.Bullet(
                                text = task.groupValues[2],
                                checked = task.groupValues[1].lowercase() == "x",
                            ),
                        )
                    } else {
                        emit(MdBlock.Bullet(text = content, checked = null))
                    }
                    index++
                }

                numberedRegex.matches(line) -> {
                    val match = numberedRegex.matchEntire(line)!!
                    emit(MdBlock.Numbered(match.groupValues[1], match.groupValues[2].trim()))
                    index++
                }

                else -> {
                    val parts = ArrayList<String>()
                    while (index < lines.size && lines[index].trim().isNotEmpty() &&
                        !isBlockStart(lines[index].trim())
                    ) {
                        parts.add(lines[index].trim())
                        index++
                    }
                    emit(MdBlock.Paragraph(parts.joinToString(" ")))
                }
            }
        }
        return blocks
    }

    private fun isBlockStart(line: String): Boolean {
        return line.startsWith("```") || line.startsWith("~~~") ||
            line.startsWith(">") || line.startsWith("- ") || line.startsWith("* ") ||
            line.startsWith("+ ") || line == "---" || line == "***" || line == "___" ||
            headingRegex.matches(line) || numberedRegex.matches(line)
    }
}

/** Potongan teks sebaris Markdown dengan gaya yang sudah diurai. */
data class InlineSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val code: Boolean = false,
    val strikethrough: Boolean = false,
    val link: Boolean = false,
)

/** Pengurai gaya sebaris: **tebal**, *miring*, `kode`, ~~coret~~, [label](tautan). */
object MarkdownInline {

    fun parse(text: String): List<InlineSpan> {
        val spans = ArrayList<InlineSpan>()
        val plain = StringBuilder()
        fun flush() {
            if (plain.isNotEmpty()) {
                spans.add(InlineSpan(plain.toString()))
                plain.clear()
            }
        }
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) || text.startsWith("__", i) -> {
                    val marker = text.substring(i, i + 2)
                    val end = text.indexOf(marker, i + 2)
                    if (end > i + 2) {
                        flush()
                        spans.add(InlineSpan(text.substring(i + 2, end), bold = true))
                        i = end + 2
                    } else {
                        plain.append(text[i]); i++
                    }
                }
                text.startsWith("~~", i) -> {
                    val end = text.indexOf("~~", i + 2)
                    if (end > i + 2) {
                        flush()
                        spans.add(InlineSpan(text.substring(i + 2, end), strikethrough = true))
                        i = end + 2
                    } else {
                        plain.append(text[i]); i++
                    }
                }
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > i + 1) {
                        flush()
                        spans.add(InlineSpan(text.substring(i + 1, end), code = true))
                        i = end + 1
                    } else {
                        plain.append(text[i]); i++
                    }
                }
                text[i] == '*' || text[i] == '_' -> {
                    val end = text.indexOf(text[i], i + 1)
                    if (end > i + 1) {
                        flush()
                        spans.add(InlineSpan(text.substring(i + 1, end), italic = true))
                        i = end + 1
                    } else {
                        plain.append(text[i]); i++
                    }
                }
                text[i] == '[' -> {
                    val labelEnd = text.indexOf(']', i + 1)
                    val urlEnd = if (labelEnd != -1 && labelEnd + 1 < text.length &&
                        text[labelEnd + 1] == '('
                    ) {
                        text.indexOf(')', labelEnd + 2)
                    } else {
                        -1
                    }
                    if (labelEnd > i + 1 && urlEnd > labelEnd) {
                        flush()
                        spans.add(InlineSpan(text.substring(i + 1, labelEnd), link = true))
                        i = urlEnd + 1
                    } else {
                        plain.append(text[i]); i++
                    }
                }
                else -> {
                    plain.append(text[i]); i++
                }
            }
        }
        flush()
        return spans
    }
}
