package dev.wndavenz.notely.utils

/** Jenis token kode untuk pewarnaan sintaks di blok kode Markdown. */
enum class CodeTokenType {
    PLAIN, KEYWORD, STRING, NUMBER, COMMENT, FUNCTION, TYPE, ANNOTATION, PROPERTY,
}

/** Potongan kode dengan jenis tokennya; menggabungkan semua potongan = kode utuh. */
data class CodeSpan(val text: String, val type: CodeTokenType)

/**
 * Penyorot sintaks ringan tanpa dependensi, untuk blok kode di dalam dokumen
 * Markdown. Satu tokenizer umum yang mengenali pola bersama keluarga bahasa
 * populer (Kotlin, Java, JavaScript/TypeScript, Python, Go, Rust, C/C++,
 * Swift, Dart, PHP, shell): kata kunci, string, angka, komentar, anotasi,
 * nama fungsi, dan nama tipe. Bahasa yang tidak dikenal tetap aman —
 * token dasar (string/komentar/angka) tetap diwarnai.
 */
object CodeHighlight {

    private val KEYWORDS = setOf(
        // Kotlin / Java / Dart / Swift / C-family
        "val", "var", "fun", "class", "object", "interface", "data", "sealed", "enum",
        "when", "if", "else", "for", "while", "do", "return", "break", "continue",
        "import", "package", "from", "as", "is", "in", "out", "null", "true", "false",
        "this", "super", "try", "catch", "finally", "throw", "throws", "suspend",
        "private", "public", "protected", "internal", "open", "final", "abstract",
        "override", "static", "void", "int", "long", "double", "float", "boolean",
        "char", "byte", "short", "const", "lateinit", "by", "lazy", "init", "companion",
        "new", "extends", "implements", "async", "await", "yield", "typeof", "instanceof",
        // JavaScript / TypeScript
        "let", "function", "arrow", "type", "namespace", "readonly", "declare",
        "export", "default", "switch", "case",
        // Python
        "def", "lambda", "None", "True", "False", "and", "or", "not", "with",
        "pass", "global", "nonlocal", "assert", "del", "elif", "except", "raise",
        "print",
        // Go / Rust / shell dll.
        "func", "go", "defer", "select", "struct", "impl", "trait", "fn", "mut",
        "match", "use", "pub", "mod", "where", "unsafe", "echo", "fi", "then",
    )

    fun highlight(code: String, language: String = ""): List<CodeSpan> {
        val spans = ArrayList<CodeSpan>()
        val plain = StringBuilder()
        fun push(text: String, type: CodeTokenType) {
            if (text.isEmpty()) return
            if (plain.isNotEmpty()) {
                spans.add(CodeSpan(plain.toString(), CodeTokenType.PLAIN))
                plain.clear()
            }
            spans.add(CodeSpan(text, type))
        }
        fun flushPlain() {
            if (plain.isNotEmpty()) {
                spans.add(CodeSpan(plain.toString(), CodeTokenType.PLAIN))
                plain.clear()
            }
        }

        val hashComment = language in setOf(
            "python", "py", "bash", "sh", "shell", "zsh", "yaml", "yml", "toml", "ruby", "rb",
        )
        var i = 0
        while (i < code.length) {
            val c = code[i]
            when {
                // Komentar baris //
                c == '/' && i + 1 < code.length && code[i + 1] == '/' -> {
                    val end = code.indexOf('\n', i).let { if (it == -1) code.length else it }
                    push(code.substring(i, end), CodeTokenType.COMMENT)
                    i = end
                }
                // Komentar blok /* */
                c == '/' && i + 1 < code.length && code[i + 1] == '*' -> {
                    val close = code.indexOf("*/", i + 2)
                    val end = if (close == -1) code.length else close + 2
                    push(code.substring(i, end), CodeTokenType.COMMENT)
                    i = end
                }
                // Komentar # (bahasa skrip)
                c == '#' && hashComment -> {
                    val end = code.indexOf('\n', i).let { if (it == -1) code.length else it }
                    push(code.substring(i, end), CodeTokenType.COMMENT)
                    i = end
                }
                // String triple """...""" atau '''...'''
                (c == '"' || c == '\'') && i + 2 < code.length &&
                    code[i + 1] == c && code[i + 2] == c -> {
                    val marker = code.substring(i, i + 3)
                    val close = code.indexOf(marker, i + 3)
                    val end = if (close == -1) code.length else close + 3
                    push(code.substring(i, end), CodeTokenType.STRING)
                    i = end
                }
                // String biasa "..." '...' `...` (hormati escape \)
                c == '"' || c == '\'' || c == '`' -> {
                    var j = i + 1
                    while (j < code.length) {
                        if (code[j] == '\\') {
                            j += 2
                            continue
                        }
                        if (code[j] == c || code[j] == '\n' && c != '`') break
                        j++
                    }
                    val end = if (j < code.length && code[j] == c) j + 1 else j
                    push(code.substring(i, end), CodeTokenType.STRING)
                    i = end
                }
                // Anotasi / dekorator @Nama
                c == '@' && i + 1 < code.length && (code[i + 1].isLetter() || code[i + 1] == '_') -> {
                    var j = i + 1
                    while (j < code.length && (code[j].isLetterOrDigit() || code[j] == '_')) j++
                    push(code.substring(i, j), CodeTokenType.ANNOTATION)
                    i = j
                }
                // Angka (desimal, hex, biner, pemisah _)
                c.isDigit() -> {
                    var j = i
                    while (j < code.length &&
                        (code[j].isLetterOrDigit() || code[j] == '.' || code[j] == '_')
                    ) {
                        j++
                    }
                    push(code.substring(i, j), CodeTokenType.NUMBER)
                    i = j
                }
                // Identitas: kata kunci / fungsi / tipe / biasa
                c.isLetter() || c == '_' -> {
                    var j = i
                    while (j < code.length && (code[j].isLetterOrDigit() || code[j] == '_')) j++
                    val word = code.substring(i, j)
                    var k = j
                    while (k < code.length && (code[k] == ' ' || code[k] == '\t')) k++
                    when {
                        word in KEYWORDS -> push(word, CodeTokenType.KEYWORD)
                        // Huruf besar di awal = nama tipe, termasuk panggilan
                        // konstruktor seperti Catatan(...) — cek sebelum fungsi.
                        word.first().isUpperCase() -> push(word, CodeTokenType.TYPE)
                        k < code.length && code[k] == '(' -> push(word, CodeTokenType.FUNCTION)
                        else -> plain.append(word)
                    }
                    i = j
                }
                else -> {
                    plain.append(c)
                    i++
                }
            }
        }
        flushPlain()
        return spans
    }
}
