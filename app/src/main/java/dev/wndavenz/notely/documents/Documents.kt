package dev.wndavenz.notely.documents

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Satu berkas dokumen (.md / .txt) yang ditemukan di penyimpanan bersama. */
data class DocumentFile(
    val name: String,
    val path: String,
    val extension: String,
    val sizeBytes: Long,
    val modifiedAt: Long,
) {
    /** Jalur relatif terhadap akar penyimpanan bersama, untuk subjudul daftar. */
    val relativePath: String
        get() {
            val root = DocumentScanner.storageRoot.path
            return if (path.startsWith(root)) {
                path.removePrefix(root).trimStart('/')
            } else {
                path
            }
        }

    val isMarkdown: Boolean get() = extension == "md" || extension == "markdown"
}

/** Akses baca ke seluruh penyimpanan bersama (untuk memindai .md / .txt). */
object DocumentAccess {

    /**
     * Android 11+ memakai "Akses semua file" (pengaturan khusus); Android 10
     * masih memakai izin baca penyimpanan klasik.
     */
    fun hasAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    /** Intent ke pengaturan "Akses semua file" untuk Notely (Android 11+). */
    fun allFilesAccessIntent(context: Context): Intent {
        val appIntent = Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
        return if (appIntent.resolveActivity(context.packageManager) != null) {
            appIntent
        } else {
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        }
    }
}

/** Pemindai rekursif penyimpanan bersama untuk berkas .md dan .txt. */
object DocumentScanner {

    val storageRoot: File get() = Environment.getExternalStorageDirectory()

    private val EXTENSIONS = setOf("md", "markdown", "txt")

    /** Batas hasil supaya daftar tetap ringan di penyimpanan yang sangat besar. */
    const val MAX_RESULTS = 5_000

    /** Batas ukuran berkas yang dibaca utuh oleh layar baca (2 MB). */
    const val MAX_READ_BYTES = 2_000_000

    /**
     * Hasil pindai terakhir (cache selama proses app hidup). Halaman Dokumen
     * menampilkannya seketika saat dibuka, lalu memindai ulang diam-diam
     * di latar — jadi tidak ada layar "Memindai…" tiap kali masuk.
     */
    @Volatile
    var cachedResults: List<DocumentFile>? = null
        private set

    /**
     * Berkas di dalam folder tersembunyi dan folder Android/ dilewati:
     * isinya bukan dokumen pengguna dan sebagian tidak bisa dijangkau.
     */
    fun isDocumentFile(file: File): Boolean {
        return file.isFile &&
            file.extension.lowercase() in EXTENSIONS &&
            !file.name.startsWith(".")
    }

    fun toDocument(file: File): DocumentFile = DocumentFile(
        name = file.name,
        path = file.absolutePath,
        extension = file.extension.lowercase(),
        sizeBytes = file.length(),
        modifiedAt = file.lastModified(),
    )

    /** Pindai seluruh penyimpanan bersama; hasil diurutkan terbaru dulu. */
    suspend fun scan(): List<DocumentFile> = withContext(Dispatchers.IO) {
        val root = storageRoot
        if (!root.exists()) return@withContext emptyList()
        val found = ArrayList<DocumentFile>()
        val walk = root.walkTopDown()
            .onEnter { dir ->
                dir == root || (!dir.name.startsWith(".") && dir.name != "Android")
            }
            .onFail { _, _ -> }
        for (file in walk) {
            if (isDocumentFile(file)) {
                found.add(toDocument(file))
                if (found.size >= MAX_RESULTS) break
            }
        }
        found.sortByDescending { it.modifiedAt }
        cachedResults = found
        found
    }

    /** Baca isi berkas sebagai teks; berkas raksasa dipotong di batas aman. */
    suspend fun readText(document: DocumentFile): DocumentRead = withContext(Dispatchers.IO) {
        val file = File(document.path)
        val bytes = file.inputStream().use { input ->
            val buffer = ByteArray(MAX_READ_BYTES + 1)
            var total = 0
            while (total < buffer.size) {
                val read = input.read(buffer, total, buffer.size - total)
                if (read == -1) break
                total += read
            }
            buffer.copyOf(total)
        }
        val truncated = bytes.size > MAX_READ_BYTES
        val usable = if (truncated) bytes.copyOf(MAX_READ_BYTES) else bytes
        DocumentRead(
            text = String(usable, Charsets.UTF_8),
            truncated = truncated,
        )
    }
}

/** Hasil baca berkas: teks (mungkin terpotong) + penanda terpotong. */
data class DocumentRead(val text: String, val truncated: Boolean)

/** Preferensi layar baca dokumen (pilihan "Ukuran Teks"), disimpan lokal. */
object ReaderPrefs {

    private const val PREFS_NAME = "reader_prefs"
    private const val KEY_TEXT_SCALE = "text_scale"

    /** Skala ukuran teks viewer: 1.0 = sedang. */
    fun textScale(context: Context): Float =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getFloat(KEY_TEXT_SCALE, 1.0f)

    fun setTextScale(context: Context, scale: Float) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_TEXT_SCALE, scale)
            .apply()
    }
}

/** Format ukuran berkas ringkas: B, KB, MB. */
fun formatFileSize(bytes: Long): String = when {
    bytes < 1_000 -> "$bytes B"
    bytes < 1_000_000 -> "%.1f KB".format(bytes / 1_000.0).replace(',', '.')
    else -> "%.1f MB".format(bytes / 1_000_000.0).replace(',', '.')
}
