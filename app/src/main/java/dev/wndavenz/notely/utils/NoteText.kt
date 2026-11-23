package dev.wndavenz.notely.utils

/** Penurunan judul dan pratinjau dari isi catatan, mengikuti perilaku Apple Notes. */
object NoteText {

    const val EMPTY_TITLE = "Catatan Baru"
    const val EMPTY_PREVIEW = "Tidak ada teks tambahan"

    /** Baris non-kosong pertama adalah judul. */
    fun titleOf(body: String): String {
        return body.lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            ?: EMPTY_TITLE
    }

    /** Baris non-kosong pertama setelah baris judul, untuk pratinjau daftar. */
    fun previewOf(body: String): String {
        var titleSeen = false
        for (line in body.lineSequence()) {
            if (!titleSeen) {
                if (line.isNotBlank()) titleSeen = true
                continue
            }
            if (line.isNotBlank()) return line.trim()
        }
        return EMPTY_PREVIEW
    }
}
